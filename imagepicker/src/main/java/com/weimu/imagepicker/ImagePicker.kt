package com.weimu.imagepicker

import android.app.Activity

import com.weimu.imagepicker.ui.CameraSelectorActivity
import com.weimu.imagepicker.ui.ImageSelectorActivity

/**
 * Author:你需要一台永动机
 * Date:2018/3/6 10:49
 * Description:
 */

object ImagePicker {

    val REQUEST_IMAGE = 66
    val REQUEST_CAMERA = 67
    val REQUEST_OUTPUT = "outputList"


    /**
     * 打开图库
     *
     * @param activity
     * @param maxSelectNum  最大选择图片数
     * @param mode          图库模式【单选】【多选】
     * @param enableCamera  是否启用摄像头
     * @param enablePreview 是否打开预览
     * @param enableCrop    是否进行裁剪【单选可用】
     */
    fun pickImage(
            activity: Activity,
            maxSelectNum: Int = 9,
            mode: Int = ImageSelectorActivity.MODE_MULTIPLE,
            enableCamera: Boolean = true,
            enablePreview: Boolean = true,
            enableCrop: Boolean = false,
            enableCompress: Boolean = true) {
        ImageSelectorActivity.start(activity, maxSelectNum, mode, enableCamera, enablePreview, enableCrop, enableCompress)
    }


    /**
     * 选择头像
     *
     * @param activity
     */
    fun pickAvatar(activity: Activity) {
        ImageSelectorActivity.start(activity, 1, ImageSelectorActivity.MODE_SINGLE, true, true, true, false)
    }


    /**
     * 使用摄像头
     *
     * @param activity
     * @param enableCrop 是否启用裁剪
     */
    fun takePhoto(activity: Activity, enableCrop: Boolean = false) {
        CameraSelectorActivity.start(activity, enableCrop)
    }

    /**
     * 自定义 启动activity
     */
    fun custom(activity: Activity, maxSelectNum: Int, mode: Int, enableCamera: Boolean, enablePreview: Boolean, enableCrop: Boolean, enableCompress: Boolean) {
        ImageSelectorActivity.start(activity, maxSelectNum, mode, enableCamera, enablePreview, enableCrop, enableCompress)
    }


    /**
     * 自定义 生成Intent
     * 注意：使用此方法 必须自己调用 startActivityForResult
     */
    fun customWithIntent(activity: Activity, maxSelectNum: Int, mode: Int, enableCamera: Boolean, enablePreview: Boolean, enableCrop: Boolean, enableCompress: Boolean) {
        ImageSelectorActivity.newIntent(activity, maxSelectNum, mode, enableCamera, enablePreview, enableCrop, enableCompress)
    }

}
