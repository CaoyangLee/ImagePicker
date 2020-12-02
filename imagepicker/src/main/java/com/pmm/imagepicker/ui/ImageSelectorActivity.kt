package com.pmm.imagepicker.ui


import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.pmm.imagepicker.*
import com.pmm.imagepicker.adapter.ImageListAdapter
import com.pmm.imagepicker.ktx.createCameraFile
import com.pmm.imagepicker.ktx.startActionCapture
import com.pmm.imagepicker.model.LocalMedia
import com.pmm.imagepicker.ui.preview.ImagePreviewActivity
import com.pmm.ui.core.StatusNavigationBar
import com.pmm.ui.core.activity.BaseActivity
import com.pmm.ui.core.dialog.ProgressDialog
import com.pmm.ui.core.recyclerview.decoration.GridItemDecoration
import com.pmm.ui.helper.FileHelper
import com.pmm.ui.ktx.*
import com.pmm.ui.widget.ToolBarPro
import id.zelory.compressor.Compressor
import kotlinx.android.synthetic.main.activity_imageselector.*
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import java.io.File
import java.util.*
import kotlin.collections.set
import kotlin.properties.Delegates
import kotlin.reflect.KProperty

/**
 * Author:你需要一台永动机
 * Date:2020/7/31 11:13
 * Description:图片选择器
 */
internal class ImageSelectorActivity : BaseActivity() {
    private val mVm by lazy { ViewModelProvider(this).get(ImageSelectorViewModel::class.java) }

    private lateinit var config: Config

    //ui
    private val mRecyclerView by lazy { findViewById<RecyclerView>(R.id.folder_list) }
    private val imageAdapter by lazy { ImageListAdapter(this, config) }
    private val mFolderName by lazy { findViewById<TextView>(R.id.tvFolderName) }
    private val folderLayout by lazy { findViewById<LinearLayout>(R.id.folder_layout) }

    private var cameraPath: String? = null

    private var isUseOrigin by Delegates.observable(false) { property: KProperty<*>, oldValue: Boolean, newValue: Boolean ->
        tvOrigin.isActivated = newValue
    }//是否使用原图

    private var isLoadImgIng = false//是否正在加载图片->返回给app

    private var loadDelay = 0L//第一次为0，后面为300毫秒，为了让共享元素动画可以正常运行

    private val folderDialog by lazy { FolderDialog(this) }

    companion object {
        const val BUNDLE_CAMERA_PATH = "CameraPath"

        //直接开启activity
        fun start(activity: Activity, config: Config) {
            val intent = Intent(activity, ImageSelectorActivity::class.java)
            intent.putExtra(Config.EXTRA_CONFIG, config)
            activity.startActivityForResult(intent, ImagePicker.REQUEST_IMAGE)
        }

        //生成新的Intent
        fun newIntent(context: Context, config: Config): Intent {
            val intent = Intent(context, ImageSelectorActivity::class.java)
            intent.putExtra(Config.EXTRA_CONFIG, config)
            return intent
        }
    }


    override fun getLayoutResID(): Int = R.layout.activity_imageselector

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(BUNDLE_CAMERA_PATH, cameraPath)
    }

    override fun beforeSuperCreate(savedInstanceState: Bundle?) {
        StatusNavigationBar.setStatusNavigationBarTransparent(window)
        //StatusBar
    }

    override fun beforeViewAttach(savedInstanceState: Bundle?) {
        config = intent.getSerializableExtra(Config.EXTRA_CONFIG) as Config
        if (savedInstanceState != null) {
            cameraPath = savedInstanceState.getString(BUNDLE_CAMERA_PATH)
        }
    }

    override fun afterViewAttach(savedInstanceState: Bundle?) {
        initRender()
        initObserver()
        initInteraction()
        mVm.loadImages(this)//加载图片
    }

    private fun initObserver() {
        mVm.foldersLiveData.observe(this, androidx.lifecycle.Observer {
            if (it != null) {
                Handler().postDelayed({
                    //跳转其他app：在切换进来也会加载
                    //load all images first
                    imageAdapter.bindImages(it[folderDialog.getFolderIndex()].images)
                    if (loadDelay == 0L) loadDelay = 350
                }, loadDelay)
            }
        })
    }


    private fun initRender() {
        //ToolBar
        mToolBar.apply {
            this.showStatusView = true
            this.navigationIcon {
                if (ToolBarPro.GlobalConfig.navigationDrawable == null) {
                    this.setImageResource(R.drawable.ic_nav_back_24dp)
                    val lightColor = this@apply.getToolBarBgColor().isLightColor()
                    this.setColorFilter(if (lightColor) Color.BLACK else Color.WHITE)
                }
                this.click { onBackPressed() }
            }
            this.centerTitle {
                this.text = getString(R.string.select_image)
            }
            this.menuText1 {
                this.text = if (config.selectMode == Config.MODE_MULTIPLE) (getString(R.string.done)) else ""
                this.click {
                    //点击完成
                    onSelectDone(imageAdapter.selectedImages)
                }
                this.invisible()
            }

        }

        StatusNavigationBar.apply {
            val statusColor = mToolBar.getToolBarBgColor()
            if (statusColor.isLightColor()) {
                this.setLightMode(window, true)
            } else {
                this.setDarkMode(window, true)
            }
        }

        //rl_navigator.setMargins(b = getNavigationBarHeight())

        //CheckBox use Origin Pic
        tvOrigin.apply {
            if (!config.showIsCompress) {
                this.visibility = View.GONE
            } else {
                this.isActivated = isUseOrigin
                this.click {
                    isUseOrigin = !isUseOrigin
                }
            }
        }
        //RecyclerView
        mRecyclerView.apply {
            this.init()
            this.layoutManager = GridLayoutManager(this@ImageSelectorActivity, config.gridSpanCount)
            this.setHasFixedSize(true)
            this.addItemDecoration(GridItemDecoration(config.gridSpanCount, dip2px(2f), dip2px(2f)))
            this.adapter = imageAdapter
            this.setPadding(dip2px(2f), 0, dip2px(2f), dip2px(48f))
        }
    }

    private fun initInteraction() {
        folderLayout.click {
            val folderList = mVm.foldersLiveData.value ?: arrayListOf()
            if (folderList.isEmpty()) {
                Toast.makeText(this@ImageSelectorActivity, R.string.no_more_folder, Toast.LENGTH_SHORT).show()
                return@click
            }
            //文件夹弹窗
            folderDialog.apply {
                folders = folderList
                //点击某个文件件
                onFolderClickListener = { folderName, images ->
                    imageAdapter.bindImages(images)
                    mFolderName.text = folderName
                    mRecyclerView.smoothScrollToPosition(0)
                }
            }.show()
        }

        //recyclerView点击事件
        imageAdapter.setOnImageSelectChangedListener(object : ImageListAdapter.OnImageSelectChangedListener {
            @SuppressLint("SetTextI18n")
            override fun onChange(selectImages: List<LocalMedia>) {
                mToolBar.menuText1 {
                    val enable = selectImages.isNotEmpty()
                    if (enable) {
                        this.text = "${getString(R.string.done_num)}(${selectImages.size}/${config.maxSelectNum})"
                        this.visible()
                    } else {
                        this.text = getString(R.string.done)
                        this.invisible()
                    }
                }
            }

            override fun onTakePhoto() {
                startCamera()
            }

            override fun onPictureClick(media: LocalMedia, position: Int, view: View) {
                when {
                    config.enablePreview -> {
//                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//                            startPreviewWithAnim(position, view)
//                        } else {
//                            startPreview(position)
//                        }
                        //为了能显示gif图
                        startPreview(position)
                    }
                    config.enableCrop -> {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            startCrop("${media.path}")
                        } else {
                            startCrop(media.path)
                        }
                    }
                    else -> {
                        onSelectDone(media.path)
                    }
                }
            }
        })
        //点击某个文件件
//        folderWindow.onFolderClickListener = { folderName, images ->
//            imageAdapter.bindImages(images)
//            mFolderName.text = folderName
//            recyclerView.smoothScrollToPosition(0)
//        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            // on take photo success
            if (requestCode == ImagePicker.REQUEST_CAMERA) {
                sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(File(cameraPath))))
                if (config.enableCrop) {
                    startCrop(cameraPath)
                } else {
                    onSelectDone(cameraPath)
                }
            } else if (requestCode == ImagePreviewActivity.REQUEST_PREVIEW) {
                val isDone = data?.getBooleanExtra(ImagePreviewActivity.OUTPUT_ISDONE, false)
                        ?: false
                val images = data?.getSerializableExtra(ImagePreviewActivity.OUTPUT_LIST) as List<LocalMedia>
                if (isDone) {
                    onSelectDone(images)
                } else {
                    if (images.isEmpty()) return
                    imageAdapter.bindSelectImages(images as ArrayList<LocalMedia>)
                }
            } else if (requestCode == ImageCropActivity.REQUEST_CROP) {
                val path = data?.getStringExtra(ImageCropActivity.OUTPUT_PATH) ?: ""
                onSelectDone(path)
            }
        }
    }

    /**
     * 打开相机，预览，裁剪
     */
    fun startCamera() {
        val cameraFile = createCameraFile()
        cameraPath = cameraFile.absolutePath
        startActionCapture(cameraFile, ImagePicker.REQUEST_CAMERA)
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    fun startPreviewWithAnim(position: Int, view: View) {
        ImagePreviewActivity.startPreviewWithAnim(this, imageAdapter.selectedImages, config.maxSelectNum, position, view)
    }

    fun startPreview(position: Int) {
        ImagePreviewActivity.startPreview(this, imageAdapter.selectedImages, config.maxSelectNum, position)
    }

    @SuppressLint("RestrictedApi")
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    fun startCropWithAnim(path: String, view: View) {
        startActivityForResult(ImageCropActivity.newIntent(this, path, config), ImageCropActivity.REQUEST_CROP,
                ActivityOptions.makeSceneTransitionAnimation(this, view, "share_image").toBundle())
    }

    fun startCrop(path: String?) {
        startActivityForResult(ImageCropActivity.newIntent(this, "$path", config), ImageCropActivity.REQUEST_CROP)
    }

    //选择完成
    private fun onSelectDone(medias: List<LocalMedia>) {
        val images = ArrayList<String>()
        for (media in medias) {
            images.add("${media.path}")
        }
        onResult(images)
    }

    fun onSelectDone(path: String?) {
        val images = ArrayList<String>()
        images.add("$path")
        onResult(images)
    }

    //返回图片
    private fun onResult(images: ArrayList<String>) {
        if (isLoadImgIng) return
        isLoadImgIng = true
        if (isUseOrigin) {
            setResult(Activity.RESULT_OK, Intent().putStringArrayListExtra(ImagePicker.REQUEST_OUTPUT, images))
            onBackPressed()
        } else {
            compressImage(images)
        }
    }

    //压缩图片
    /**
     * 压缩图片，暂时不支持GIf压缩，遇到Gif图片直接返回原地址
     */
    private fun compressImage(photos: ArrayList<String>) {
        if (photos.size > 9) ProgressDialog.show(this@ImageSelectorActivity, message = "加载中")
        val newImageList = ArrayList<String>()

        val compressImg = arrayListOf<String>()
        val gifMap = hashMapOf<String, Int>()//记录一下gif的位置
        for ((idx, item) in photos.withIndex()) {
            if (item.endsWith(".gif"))
                gifMap[item] = idx
            else
                compressImg.add(item)
        }
        //结束选择
        fun finishSelect() {
            ProgressDialog.hide()
            for (item in gifMap) {
                if (item.value >= compressImg.size)
                    newImageList.add(item.key)
                else
                    newImageList.add(item.value, item.key)
            }
            isLoadImgIng = false
            setResult(Activity.RESULT_OK, Intent().putStringArrayListExtra(ImagePicker.REQUEST_OUTPUT, newImageList))
            onBackPressed()
        }

        if (compressImg.isEmpty()) {
            finishSelect()
        } else {
            MainScope().launch {
                for (image in compressImg) {
                    Log.d("imagePicker", "--------------------------------------------- >>>")
                    Log.d("imagePicker", "压缩前：")
                    Log.d("imagePicker", "地址：$image")
                    Log.d("imagePicker", "文件大小：${FileHelper.getFileSize(File(image))}")
                    val compressedImg = Compressor.compress(this@ImageSelectorActivity, File(image))
                    Log.d("imagePicker", "压缩后：")
                    Log.d("imagePicker", "地址：$compressedImg")
                    Log.d("imagePicker", "文件大小：${FileHelper.getFileSize(compressedImg)}")
                    Log.d("imagePicker", "<<< ---------------------------------------------")
                    newImageList.add(compressedImg.toString())
                }
                finishSelect()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ImageStaticHolder.clearImages()
    }

}
