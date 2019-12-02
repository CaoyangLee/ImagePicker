package com.pmm.imagepicker.ui.preview

import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityOptions
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.viewpager.widget.ViewPager
import com.pmm.imagepicker.ImageStaticHolder
import com.pmm.imagepicker.R
import com.pmm.imagepicker.model.ImageData
import com.pmm.ui.core.activity.BaseActivity
import com.pmm.ui.core.pager.BaseFragmentStatePagerAdapter
import com.pmm.ui.core.toolbar.StatusBarManager
import com.pmm.ui.ktx.*
import com.pmm.ui.widget.ToolBarPro
import kotlinx.android.synthetic.main.activity_image_preview.*
import java.util.*
import kotlin.properties.Delegates
import kotlin.reflect.KProperty


internal class ImagePreviewActivity : BaseActivity() {

    companion object {
        val REQUEST_PREVIEW = 68
        val EXTRA_PREVIEW_SELECT_LIST = "previewSelectList"
        val EXTRA_MAX_SELECT_NUM = "maxSelectNum"
        val EXTRA_POSITION = "position"

        val OUTPUT_LIST = "outputList"
        val OUTPUT_ISDONE = "isDone"


        fun startPreview(context: Activity, selectImages: List<ImageData>, maxSelectNum: Int, position: Int) {
            val intent = Intent(context, ImagePreviewActivity::class.java)
            intent.putExtra(EXTRA_PREVIEW_SELECT_LIST, selectImages as ArrayList<ImageData>)

            intent.putExtra(EXTRA_POSITION, position)
            intent.putExtra(EXTRA_MAX_SELECT_NUM, maxSelectNum)
            context.startActivityForResult(intent, REQUEST_PREVIEW)
        }


        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        fun startPreviewWithAnim(context: Activity, selectImages: List<ImageData>, maxSelectNum: Int, position: Int, view: View) {
            val intent = Intent(context, ImagePreviewActivity::class.java)
            intent.putExtra(EXTRA_PREVIEW_SELECT_LIST, selectImages as ArrayList<ImageData>)

            intent.putExtra(EXTRA_POSITION, position)
            intent.putExtra(EXTRA_MAX_SELECT_NUM, maxSelectNum)
            context.startActivityForResult(intent, REQUEST_PREVIEW, ActivityOptions.makeSceneTransitionAnimation(context, view, "share_image").toBundle())
        }
    }

    private val viewPager: ViewPager by lazy { findViewById<ViewPager>(R.id.drag_viewPager) }

    private var position: Int = 0
    private var maxSelectNum = 1
    private var images: List<ImageData> = ArrayList()//所有图片
    private var selectImages: ArrayList<ImageData> = ArrayList()//选择的图片


    var isShowBar by Delegates.observable(false) { property, oldValue, newValue ->
        //todo是否显示
        if (newValue) {
            mToolBar.visible()
            mSelectBarLayout.visible()
            StatusBarManager.showStatusBar(window)
        } else {
            mToolBar.gone()
            mSelectBarLayout.gone()
            StatusBarManager.hideStatusBar(window)
        }
    }

    private var isSelected by Delegates.observable(false) { property: KProperty<*>, oldValue: Boolean, newValue: Boolean ->
        mTvSelect.isActivated = newValue
    }//是否选中

    override fun getLayoutResID(): Int = R.layout.activity_image_preview


    override fun beforeViewAttach(savedInstanceState: Bundle?) {
        //images = getIntent().getParcelableArrayListExtra(EXTRA_PREVIEW_LIST);
        images = ImageStaticHolder.getChooseImages()
        selectImages = intent.getSerializableExtra(EXTRA_PREVIEW_SELECT_LIST) as ArrayList<ImageData>
        maxSelectNum = intent.getIntExtra(EXTRA_MAX_SELECT_NUM, 9)
        position = intent.getIntExtra(EXTRA_POSITION, 1)
    }

    override fun afterViewAttach(savedInstanceState: Bundle?) {
        initView()
        registerListener()
    }


    private fun initView() {
        mToolBar.apply {
            this.navigationIcon {
                if (ToolBarPro.GlobalConfig.navigationDrawable == null) {
                    this.setImageResource(R.drawable.ic_nav_back_24dp)
                    val lightColor = this@apply.getToolBarBgColor().isLightColor()
                    this.setColorFilter(if (lightColor) Color.BLACK else Color.WHITE)
                }
                this.click { onBackPressed() }
            }
            this.centerTitle {
                this.text = "${position + 1}/${images.size}"
            }
            this.menuText1 {
                this.text = getString(R.string.done)
                this.click {
                    //点击完成
                    onDoneClick(true)
                }
                this.invisible()
            }
        }

        //StatusBar
        StatusBarManager.apply {
            val statusColor = mToolBar.getToolBarBgColor()
            this.setColor(window, statusColor)
            if (statusColor.isLightColor()) {
                this.setLightMode(window)
            } else {
                this.setDarkMode(window)
            }
        }

        onSelectNumChange()

        onImageSwitch(position)


        viewPager.apply {
            this.offscreenPageLimit = 3
            this.adapter = SimpleFragmentAdapter(supportFragmentManager)
            this.currentItem = position
            //触发接口
            this.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
                override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}

                override fun onPageSelected(position: Int) {
                    mToolBar.centerTitle { this.text = ((position + 1).toString() + "/" + images.size) }
                    onImageSwitch(position)
                }

                override fun onPageScrollStateChanged(state: Int) {}
            })
        }


    }

    @SuppressLint("StringFormatMatches")
    fun registerListener() {
        mTvSelect.setOnClickListener(View.OnClickListener {
            isSelected = !isSelected
            if (selectImages.size >= maxSelectNum && isSelected) {
                Toast.makeText(this@ImagePreviewActivity, getString(R.string.message_max_num, maxSelectNum), Toast.LENGTH_LONG).show()
                mTvSelect.isActivated = false
                return@OnClickListener
            }
            val image = images[viewPager.currentItem]
            if (isSelected) {
                selectImages.add(image)
            } else {
                for (media in selectImages) {
                    if (media.path == image.path) {
                        selectImages.remove(media)
                        break
                    }
                }
            }
            onSelectNumChange()
        })
    }

    inner class SimpleFragmentAdapter(fm: FragmentManager) : BaseFragmentStatePagerAdapter(fm) {
        override fun getItem(position: Int): Fragment = ImagePreviewFragment.newInstance(images[position].path, images[position].uri)
        override fun getCount(): Int = images.size
    }


    @SuppressLint("SetTextI18n")
    fun onSelectNumChange() {
        val enable = selectImages.size != 0
        mToolBar.menuText1 {
            if (enable) {
                this.visible()
                this.text = "${getString(R.string.done_num)}(${selectImages.size}/${maxSelectNum})"
            } else {
                this.invisible()
                this.text = getString(R.string.done)
            }
        }
    }

    fun onImageSwitch(position: Int) {
        for (media in selectImages) {
            if (media.path == images[position].path) {
                isSelected = true
                return
            }
        }
        isSelected = false
    }


    private fun onDoneClick(isDone: Boolean) {
        val intent = Intent()
        intent.putExtra(OUTPUT_LIST, selectImages as ArrayList<*>)
        intent.putExtra(OUTPUT_ISDONE, isDone)
        setResult(Activity.RESULT_OK, intent)
        super.onBackPressed()
        //overridePendingTransition(-1, R.anim.fade_out)
    }

    override fun onBackPressed() {
        onDoneClick(false)
    }

}
