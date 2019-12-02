package com.pmm.imagepicker.model

import android.net.Uri
import android.os.Parcel
import android.os.Parcelable


class ImageData(
        @Deprecated("Android10后开始弃用")
        val path: String?,
        val uri: Uri?
) : Parcelable, Comparable<ImageData> {

    constructor(parcel: Parcel) : this(
            parcel.readString(),
            parcel.readParcelable(Uri::class.java.classLoader)) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(path)
        parcel.writeParcelable(uri, flags)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<ImageData> {
        override fun createFromParcel(parcel: Parcel): ImageData {
            return ImageData(parcel)
        }

        override fun newArray(size: Int): Array<ImageData?> {
            return arrayOfNulls(size)
        }
    }

    override fun compareTo(other: ImageData): Int = -((path ?: "").compareTo(other.path ?: ""))
}
