package com.pmm.imagepicker.ui

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.PopupWindow
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.pmm.imagepicker.R
import com.pmm.imagepicker.adapter.FolderClickCallBack
import com.pmm.imagepicker.adapter.ImageFolderAdapter
import com.pmm.imagepicker.model.ImageData
import com.pmm.imagepicker.model.LocalMediaFolder
import com.pmm.ui.core.recyclerview.decoration.LinearItemDecoration
import com.pmm.ui.ktx.dip2px
import com.pmm.ui.ktx.getScreenHeight
import com.pmm.ui.ktx.getScreenWidth
import com.pmm.ui.ktx.getStatusBarHeight
import com.pmm.ui.widget.ToolBarPro
import java.lang.reflect.Method

/**
 * Author:你需要一台永动机
 * Date:2019-05-21 15:25
 * Description:文件夹列表 popWindow
 */
internal class FolderWindow(private val context: Context) : PopupWindow() {
    private val window: View
    private lateinit var recyclerView: RecyclerView
    private val mAdapter: ImageFolderAdapter by lazy { ImageFolderAdapter(context) }

    var onFolderClickListener: ((folderName: String?, images: List<ImageData>) -> Unit)? = null

    private var isDismiss = false

    var folders: List<LocalMediaFolder> = arrayListOf()
        //所有图片文件夹
        private set

    var folderIndex = 0
        //文件夹对应的角标
        private set

    fun getFolderImages(position: Int=folderIndex) = folders[position].images

    fun isEmpty() = folders.isEmpty() || folders[0].images.isEmpty()

    init {
        window = LayoutInflater.from(context).inflate(R.layout.window_folder, null)
        val view = window.findViewById<View>(R.id.lin_parent)
        view.setOnClickListener { dismiss() }
        this.contentView = window
        this.width = context.getScreenWidth()
        this.height = context.getScreenHeight() - (ToolBarPro.GlobalConfig.toolbarHeight + context.getStatusBarHeight() + context.dip2px(48f))
        this.animationStyle = R.style.WindowStyle
        this.isFocusable = true
        this.isOutsideTouchable = true
        this.isClippingEnabled = false
        this.update()
        this.setBackgroundDrawable(ColorDrawable(Color.argb(153, 0, 0, 0)))

        initView()
        setPopupWindowTouchModal(this, false)
    }

    private fun initView() {
        recyclerView = (window.findViewById<View>(R.id.folder_list) as RecyclerView).apply {
            this.addItemDecoration(LinearItemDecoration(
                    context = context,
                    dividerSize = 1,
                    dividerDrawable = ColorDrawable(Color.rgb(220, 220, 220))))
            this.layoutManager = LinearLayoutManager(context)
            this.adapter = mAdapter
            this.visibility = View.GONE
        }

        mAdapter.onFolderClickListener = { index, folderName, images ->
            this.dismiss()
            folderIndex = index
            onFolderClickListener?.invoke(folderName,images)
        }
    }

    fun bindFolder(folders: List<LocalMediaFolder>) {
        this.folders = folders
        mAdapter.setDataToAdapter(this.folders)
    }

    fun setOnFolderClickListener(onFolderClickListener: FolderClickCallBack) {
        mAdapter.onFolderClickListener = onFolderClickListener
    }

    override fun showAsDropDown(anchor: View) {
        super.showAsDropDown(anchor)
        Handler().postDelayed({
            recyclerView.visibility = View.VISIBLE
            val animation = AnimationUtils.loadAnimation(context, R.anim.up_in)
            recyclerView.startAnimation(animation)
        }, 300)

    }


    override fun dismiss() {
        if (isDismiss) return
        isDismiss = true
        val animation = AnimationUtils.loadAnimation(context, R.anim.down_out)
        recyclerView.startAnimation(animation)
        dismiss()
        animation.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) {

            }

            override fun onAnimationEnd(animation: Animation) {
                isDismiss = false
                recyclerView.visibility = View.GONE
                super@FolderWindow.dismiss()
            }

            override fun onAnimationRepeat(animation: Animation) {

            }
        })
    }


    companion object {

        fun setPopupWindowTouchModal(popupWindow: PopupWindow?, touchModal: Boolean) {
            if (null == popupWindow) {
                return
            }
            val method: Method
            try {
                method = PopupWindow::class.java.getDeclaredMethod("setTouchModal", Boolean::class.javaPrimitiveType!!)
                method.isAccessible = true
                method.invoke(popupWindow, touchModal)
            } catch (e: Exception) {
                e.printStackTrace()
            }

        }
    }

}
