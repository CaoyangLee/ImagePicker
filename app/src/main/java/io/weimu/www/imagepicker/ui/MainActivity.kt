package io.weimu.www.imagepicker.ui

import android.Manifest
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.pmm.imagepicker.ImagePicker
import com.pmm.imagepicker.model.ImageData
import com.pmm.imagepicker.ui.preview2.ImagePreviewActivity
import com.pmm.ui.core.recyclerview.decoration.GridItemDecoration
import com.pmm.ui.core.toolbar.StatusBarManager
import com.pmm.ui.ktx.dip2px
import com.pmm.ui.ktx.getColorPro
import com.pmm.ui.ktx.requestPermission
import com.pmm.ui.ktx.click
import io.weimu.www.imagepicker.R
import io.weimu.www.imagepicker.fragment.adapter.ImageGridAdapter
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private val maxImageNumber = 9
    private val spanCount = 4

    private val mAdapter: ImageGridAdapter by lazy {
        ImageGridAdapter(this, maxImageNumber).apply {
            //其他操作
            this.imageActionListener = object : ImageGridAdapter.ImageActionListener {

                override fun onItemClick(position: Int) {
//                    ImagePreviewActivity.start(this@MainActivity, this@apply.dataList, position)
                }

                override fun onItemDeleteClick(position: Int) {
                    this@apply.removeItem(position)
                }

                //添加
                override fun onItemAddClick() {
//                    ImagePicker.pickAvatar(this@MainActivity);
                    ImagePicker.pickImage(this@MainActivity, 30)
//                    ImagePicker.pickImage4One(
//                            activity = this@MainActivity,
//                            cropAspectRatioY = 9,
//                            cropAspectRatioX = 16,
//                            cropMiniHeight = this@MainActivity.dip2px(100f),
//                            cropMiniWidth = this@MainActivity.dip2px(1000f)
//                    )
//                ImagePicker.takePhoto(this@MainActivity, true);//使用摄像头


//                ImagePicker.custom(this@MainActivity, Config().apply {
//                    selectMode = Config.MODE_SINGLE
//                    enableCrop = true
//                    cropAspectRatioX = 1
//                    cropAspectRatioY = 1
//                })
                }
            }

        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        StatusBarManager.setColor(window, getColorPro(R.color.colorPrimary))
        StatusBarManager.setDarkMode(window)

        //ToolBar
        mToolBar.apply {
            this.navigationIcon {
                this.click { onBackPressed() }
            }
            this.centerTitle {
                this.text = getString(R.string.app_name)
            }
        }


        requestPermission(
                permissions = *arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA),
                granted = { initRecyclerVIew() },
                content = "请给我权限！"
        )
    }

    private fun initRecyclerVIew() {
        this.id_RecyclerView.apply {
            this.adapter = mAdapter
            this.layoutManager = GridLayoutManager(this@MainActivity, spanCount)
            this.addItemDecoration(GridItemDecoration(spanCount, dip2px(8f), dip2px(8f)))
        }
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && requestCode == ImagePicker.REQUEST_IMAGE) {
            val target = data?.getParcelableArrayListExtra<ImageData>(ImagePicker.REQUEST_OUTPUT)
            mAdapter.addData(target)
        }
    }
}
