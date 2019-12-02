package com.pmm.imagepicker.model

import android.net.Uri
import java.io.Serializable
import java.util.ArrayList


internal class LocalMediaFolder : Serializable {
    var name: String? = null
    var path: String? = null
    var firstImagePath: String? = null
    var firstImageUri: Uri? = null
    var imageNum: Int = 0
    var images: List<ImageData> = ArrayList()
}
