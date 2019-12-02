package com.pmm.imagepicker.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import android.widget.FrameLayout
import com.pmm.imagepicker.Config
import com.pmm.imagepicker.ImagePicker
import com.pmm.imagepicker.R
import com.pmm.imagepicker.ktx.createCameraFile
import com.pmm.imagepicker.ktx.startActionCapture
import com.pmm.ui.core.activity.BaseActivity
import com.pmm.ui.ktx.gone
import top.zibin.luban.Luban
import top.zibin.luban.OnCompressListener
import java.io.File
import java.util.*

/**
 * 相机选择
 * Android 10兼容 @link https://juejin.im/post/5d80edb76fb9a06b1c746176
 */
internal class CameraSelectorActivity : BaseActivity() {
    companion object {
        val REQUEST_OUTPUT = "outputList"

        val BUNDLE_CAMERA_PATH = "CameraPath"

        fun start(activity: Activity, config: Config) {
            val intent = Intent(activity, CameraSelectorActivity::class.java)
            intent.putExtra(Config.EXTRA_CONFIG, config)
            activity.startActivityForResult(intent, ImagePicker.REQUEST_IMAGE)
        }
    }

    private var cameraPath: String? = null

    private lateinit var config: Config

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(BUNDLE_CAMERA_PATH, cameraPath)
    }

    override fun getLayoutUI(): ViewGroup = FrameLayout(this@CameraSelectorActivity).apply { this.gone() }

    override fun beforeViewAttach(savedInstanceState: Bundle?) {
        config = intent.getSerializableExtra(Config.EXTRA_CONFIG) as Config
        if (savedInstanceState != null) {
            cameraPath = savedInstanceState.getString(BUNDLE_CAMERA_PATH)
        }
    }

    override fun afterViewAttach(savedInstanceState: Bundle?) {
        startCamera()
    }

    /**
     * start to camera、preview、crop
     */
    private fun startCamera() {
        val cameraFile = createCameraFile()
        cameraPath = cameraFile.absolutePath
        startActionCapture(cameraFile, ImagePicker.REQUEST_CAMERA)
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
            } // on crop success
            else if (requestCode == ImageCropActivity.REQUEST_CROP) {
                val path = data!!.getStringExtra(ImageCropActivity.OUTPUT_PATH)
                onSelectDone(path)
            }
        } else {
            onBackPressed()
        }
    }


    private fun startCrop(path: String?) {
        if (path.isNullOrBlank()) return
        startActivityForResult(ImageCropActivity.newIntent(this, path, config), ImageCropActivity.REQUEST_CROP)
    }

    private fun onSelectDone(path: String?) {
        val images = ArrayList<String>()
        images.add(path!!)
        compressImage(images)//默认压缩
    }


    //压缩图片
    private fun compressImage(photos: ArrayList<String>) {
        val newImageList = ArrayList<String>()
        Luban.with(this)
                .load(photos)                                   // 传人要压缩的图片列表
                .ignoreBy(100)                                  // 忽略不压缩图片的大小
                .setCompressListener(object : OnCompressListener { //设置回调
                    override fun onStart() {
                        //Log.d("weimu", "开始压缩")
                    }

                    override fun onSuccess(file: File) {
                        //Log.d("weimu", "压缩成功 地址为：$file")
                        newImageList.add(file.toString())
                        //所有图片压缩成功
                        if (newImageList.size == photos.size) {
                            setResult(Activity.RESULT_OK, Intent().putStringArrayListExtra(REQUEST_OUTPUT, newImageList))
                            onBackPressed()
                        }
                    }

                    override fun onError(e: Throwable) {
                        e.printStackTrace()
                    }
                }).launch()    //启动压缩
    }

    override fun onBackPressed() {
        finish()
        overridePendingTransition(0, R.anim.transparent)
    }


}
