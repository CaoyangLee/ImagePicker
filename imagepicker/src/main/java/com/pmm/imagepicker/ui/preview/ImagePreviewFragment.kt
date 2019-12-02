package com.pmm.imagepicker.ui.preview

import android.net.Uri
import android.os.Bundle
import com.pmm.imagepicker.R
import com.pmm.ui.core.fragment.BaseFragment
import com.pmm.ui.ktx.load
import kotlinx.android.synthetic.main.fragment_image_preview.*


internal class ImagePreviewFragment : BaseFragment() {

    private var path = ""
    private var uri: Uri? = null

    companion object {

        val PATH = "path"
        val URI = "uri"

        fun newInstance(path: String?, uri: Uri?): ImagePreviewFragment {
            val fragment = ImagePreviewFragment()
            val bundle = Bundle()
            bundle.putString(PATH, path)
            bundle.putParcelable(URI, uri)
            fragment.arguments = bundle
            return fragment
        }
    }

    override fun getLayoutResID(): Int = R.layout.fragment_image_preview

    override fun beforeViewAttach(savedInstanceState: Bundle?) {
        path = "${arguments!!.getString(PATH)}"
        uri = arguments!!.getParcelable(URI)
    }

    override fun afterViewAttach(savedInstanceState: Bundle?) {
        photo_view.load(uri!!)
        photo_view.setOnPhotoTapListener { view, x, y ->
            val activity = activity as ImagePreviewActivity
            activity.isShowBar = !activity.isShowBar
        }

        mTvTest.text = path + "\n" + uri
    }


}
