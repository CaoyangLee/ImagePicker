package com.pmm.imagepicker.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PorterDuff
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.pmm.imagepicker.Config
import com.pmm.imagepicker.ImageStaticHolder
import com.pmm.imagepicker.R
import com.pmm.imagepicker.model.ImageData
import com.pmm.ui.ktx.load4CenterCrop
import kotlin.collections.ArrayList


internal class ImageListAdapter(
        private var context: Context,
        private val config: Config) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var images: List<ImageData> = ArrayList()
        private set
    private var selectImages: ArrayList<ImageData> = ArrayList()

    private var imageSelectChangedListener: OnImageSelectChangedListener? = null

    val selectedImages: ArrayList<ImageData>
        get() = selectImages


    fun bindImages(images: List<ImageData>) {
        //讲选中的集合引用 给到appData
        ImageStaticHolder.setChooseImages(images)
        this.images = images
        notifyDataSetChanged()
    }

    fun bindSelectImages(images: ArrayList<ImageData>) {
        this.selectImages = images
        notifyDataSetChanged()
        imageSelectChangedListener?.onChange(selectImages)
    }

    override fun getItemViewType(position: Int): Int {
        return if (config.enableCamera && position == 0) {
            TYPE_CAMERA
        } else {
            TYPE_PICTURE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        if (viewType == TYPE_CAMERA) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.list_item_camera, parent, false)
            return HeaderViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.list_item_picture, parent, false)
            return ViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (getItemViewType(position) == TYPE_CAMERA) {
            val headerHolder = holder as HeaderViewHolder
            headerHolder.headerView.setOnClickListener {
                imageSelectChangedListener?.onTakePhoto()
            }
        } else {
            val contentHolder = holder as ViewHolder
            val image = images[if (config.enableCamera) position - 1 else position]

            contentHolder.picture.load4CenterCrop(
                    uri = image.uri!!,
                    placeholder = R.drawable.ic_image_24dp
            )

            if (config.selectMode == Config.MODE_SINGLE) {
                contentHolder.ivCheckCircle.visibility = View.GONE
            }

            selectImage(contentHolder, isSelected(image))

            if (config.enablePreview) {
                contentHolder.ivCheckCircle.setOnClickListener { changeCheckboxState(contentHolder, image) }
            }

            contentHolder.contentView.setOnClickListener {
                if ((config.selectMode == Config.MODE_SINGLE || config.enablePreview)) {
                    imageSelectChangedListener?.onPictureClick(image, if (config.enableCamera) position - 1 else position, holder.picture)
                } else {
                    changeCheckboxState(contentHolder, image)
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return if (config.enableCamera) images.size + 1 else images.size
    }

    @SuppressLint("StringFormatMatches")
    private fun changeCheckboxState(contentHolder: ViewHolder, image: ImageData) {
        val isChecked = contentHolder.ivCheckCircle.isActivated
        if (selectImages.size >= config.maxSelectNum && !isChecked) {
            Toast.makeText(context, context.getString(R.string.message_max_num, config.maxSelectNum), Toast.LENGTH_LONG).show()
            return
        }
        if (isChecked) {
            for (media in selectImages) {
                if (media.path == image.path) {
                    selectImages.remove(media)
                    break
                }
            }
        } else {
            selectImages.add(image)
        }
        selectImage(contentHolder, !isChecked)
        imageSelectChangedListener?.onChange(selectImages)
    }

    private fun isSelected(image: ImageData): Boolean {
        for (media in selectImages) {
            if (media.path == image.path) {
                return true
            }
        }
        return false
    }

    private fun selectImage(holder: ViewHolder, isChecked: Boolean) {
        holder.ivCheckCircle.isActivated = isChecked
        if (isChecked) {
            holder.picture.setColorFilter(ContextCompat.getColor(context, R.color.image_overlay2), PorterDuff.Mode.SRC_ATOP)
        } else {
            holder.picture.setColorFilter(ContextCompat.getColor(context, R.color.image_overlay), PorterDuff.Mode.SRC_ATOP)
        }
    }

    internal class HeaderViewHolder(var headerView: View) : RecyclerView.ViewHolder(headerView)

    inner class ViewHolder(var contentView: View) : RecyclerView.ViewHolder(contentView) {
        var picture: ImageView = contentView.findViewById(R.id.picture)
        var ivCheckCircle: ImageView = contentView.findViewById(R.id.iv_check_circle)
    }

    interface OnImageSelectChangedListener {
        fun onChange(selectImages: List<ImageData>)

        fun onTakePhoto()

        fun onPictureClick(media: ImageData, position: Int, view: View)
    }

    fun setOnImageSelectChangedListener(imageSelectChangedListener: OnImageSelectChangedListener) {
        this.imageSelectChangedListener = imageSelectChangedListener
    }

    companion object {
        val TYPE_CAMERA = 1
        val TYPE_PICTURE = 2
    }
}
