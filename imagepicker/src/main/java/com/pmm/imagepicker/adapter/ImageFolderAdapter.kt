package com.pmm.imagepicker.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import com.pmm.imagepicker.R
import com.pmm.imagepicker.model.ImageData
import com.pmm.imagepicker.model.LocalMediaFolder
import com.pmm.ui.core.BaseB
import com.pmm.ui.core.recyclerview.BaseRecyclerAdapter
import com.pmm.ui.core.recyclerview.BaseRecyclerViewHolder
import com.pmm.ui.ktx.click
import com.pmm.ui.ktx.load4CenterCrop
import kotlinx.android.synthetic.main.list_item_folder.view.*

/**
 * Author:你需要一台永动机
 * Date:2019-05-27 15:48
 * Description:文件夹适配器
 */

internal typealias FolderClickCallBack = ((index: Int, folderName: String?, images: List<ImageData>) -> Unit)?

internal class ImageFolderAdapter(mContext: Context) : BaseRecyclerAdapter<BaseB, LocalMediaFolder>(mContext) {

    override fun getItemLayoutRes(): Int = R.layout.list_item_folder

    private var checkedIndex = -1//选中的位置
    var onFolderClickListener: FolderClickCallBack = null


    @SuppressLint("StringFormatMatches")
    override fun itemViewChange(holder: BaseRecyclerViewHolder, position: Int) {
        val item = getItem(position)
        holder.itemView.apply {
            //图片
            this.first_image.load4CenterCrop(
                    uri = item.firstImageUri!!,
                    placeholder = R.drawable.ic_image_24dp
            )
            //文件夹 名称
            this.folder_name.text = item.name
            //文件夹的图片数
            this.image_num.text = context.getString(R.string.num_postfix, item.imageNum)
            //是否显示
            this.is_selected.visibility = if (checkedIndex == position) View.VISIBLE else View.GONE
            //点击事件
            this.click {
                checkedIndex = position
                notifyDataSetChanged()
                onFolderClickListener?.invoke(position, item.name, item.images)
            }
        }
    }
}


