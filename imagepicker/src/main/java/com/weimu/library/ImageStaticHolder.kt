package com.weimu.library

import com.weimu.library.model.LocalMedia

/**
 * Author:你需要一台永动机
 * Date:2018/5/10 11:38
 * Description:
 */
object ImageStaticHolder {
    //共享选择图片列表
    private var chooseImages: MutableList<LocalMedia> = arrayListOf()

    fun getChooseImages(): List<LocalMedia> {
        return chooseImages
    }

    fun setChooseImages(chooseImages: MutableList<LocalMedia>) {
        ImageStaticHolder.chooseImages = chooseImages
    }

    fun clearImages() {
        chooseImages.clear()
    }

}
