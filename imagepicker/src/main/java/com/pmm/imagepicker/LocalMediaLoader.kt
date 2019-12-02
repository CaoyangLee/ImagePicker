package com.pmm.imagepicker

import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.fragment.app.FragmentActivity
import androidx.loader.app.LoaderManager
import androidx.loader.content.CursorLoader
import androidx.loader.content.Loader
import com.pmm.imagepicker.ktx.getImageContentUri
import com.pmm.imagepicker.model.ImageData
import com.pmm.imagepicker.model.LocalMediaFolder
import java.io.File
import java.util.*


internal class LocalMediaLoader(private val activity: FragmentActivity, var type: Int = TYPE_IMAGE) {


    private val mDirPaths = HashSet<String>()//文件夹路径

    fun loadAllImage(imageLoadListener: LocalMediaLoadListener) {
        LoaderManager.getInstance(activity).initLoader(type, null, object : LoaderManager.LoaderCallbacks<Cursor> {

            override fun onCreateLoader(id: Int, args: Bundle?): Loader<Cursor> {
                var cursorLoader: CursorLoader? = null
                when (id) {
                    TYPE_IMAGE -> {
                        cursorLoader = CursorLoader(
                                activity, MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                IMAGE_PROJECTION, MediaStore.Images.Media.MIME_TYPE + "=? or "
                                + MediaStore.Images.Media.MIME_TYPE + "=?",
                                arrayOf("image/jpeg", "image/png"), IMAGE_PROJECTION[2] + " DESC")
                    }
                    TYPE_VIDEO -> {
                        cursorLoader = CursorLoader(
                                activity, MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                                VIDEO_PROJECTION, null, null, VIDEO_PROJECTION[2] + " DESC")
                    }
                }
                return cursorLoader as Loader<Cursor>
            }

            override fun onLoadFinished(loader: Loader<Cursor>, data: Cursor?) {
                if (data == null || data.isClosed) return

                val imageFolders = ArrayList<LocalMediaFolder>()//一组文件夹
                val imageFolder4All = LocalMediaFolder()//全部图片-文件夹
                val allImages = ArrayList<ImageData>()//图片

                if (!data.moveToFirst()) {
                    return; }//issue链接：https://github.com/jeasonlzy/ImagePicker/issues/243#issuecomment-380353956

                //while循环
                do {
                    val path = data.getString(data.getColumnIndex(MediaStore.Images.Media.DATA))// 图片的路径
                    val id = data.getInt(data.getColumnIndex(MediaStore.MediaColumns._ID))
                    val baseUri = Uri.parse("content://media/external/images/media")
                    val uri = Uri.withAppendedPath(baseUri, "" + id)

//                    val uri = activity.getImageContentUri(path)

                    allImages.add(ImageData(path, uri))


                    val file = File(path)
                    if (!file.exists())
                        continue
                    // 获取该图片的目录路径名
                    val parentFile = file.parentFile//图片对应的文件夹
                    if (parentFile == null || !parentFile.exists())
                        continue

                    val dirPath = parentFile.absolutePath//文件夹路径

                    // 利用一个HashSet防止多次扫描同一个文件夹
                    if (mDirPaths.contains(dirPath)) {
                        continue
                    } else {
                        mDirPaths.add(dirPath)
                    }

                    if (parentFile.list() == null)
                        continue

                    //处理文件夹数据
                    val localMediaFolder = getImageFolder(path, imageFolders)

                    //获取文件夹里的所有图片
                    val files = parentFile.listFiles { dir, filename ->
                        filename.endsWith(".jpg") or filename.endsWith(".png") || filename.endsWith(".jpeg")
                    }

                    val images = ArrayList<ImageData>()

                    for (i in files.indices) {
                        //allImages.add(localMedia);
                        val path2 = files[i].absolutePath
                        val uri2 = activity.getImageContentUri(path2)

                        images.add(ImageData(path2, uri2))
                    }
                    if (images.size > 0) {
                        images.sort()
                        localMediaFolder.images = images
                        localMediaFolder.firstImagePath = images[0].path
                        localMediaFolder.firstImageUri = images[0].uri
                        localMediaFolder.imageNum = localMediaFolder.images.size
                        imageFolders.add(localMediaFolder)
                    }
                } while (data.moveToNext())

                imageFolder4All.images = allImages
                imageFolder4All.imageNum = imageFolder4All.images.size
                if (allImages.size != 0) {
                    imageFolder4All.firstImagePath = allImages[0].path
                    imageFolder4All.firstImageUri = allImages[0].uri
                }
                imageFolder4All.name = activity.getString(R.string.all_image)
                imageFolders.add(imageFolder4All)
                sortFolder(imageFolders)

                //加载所有文件夹
                imageLoadListener.loadComplete(imageFolders)

                mDirPaths.clear()//防止下次加载 文件夹扫描不到

                //data.close()// 不用手动关闭
            }

            override fun onLoaderReset(loader: Loader<Cursor>) {}
        })
    }

    private fun sortFolder(imageFolders: List<LocalMediaFolder>) {
        // 文件夹按图片数量排序
        Collections.sort(imageFolders, Comparator { lhs, rhs ->
            //默认升序
            if (lhs.name == activity.getString(R.string.all_image)) return@Comparator -1
            val lsize = lhs.imageNum
            val rsize = rhs.imageNum
            if (lsize == rsize) 0 else if (lsize < rsize) 1 else -1
        })
    }

    private fun getImageFolder(path: String, imageFolders: List<LocalMediaFolder>): LocalMediaFolder {
        val imageFile = File(path)
        val folderFile = imageFile.parentFile//图片对应的文件夹
        //搜寻所有文件夹
        for (folder in imageFolders) {
            //不要单用文件名比较，因为有可能出现同名不同的文件夹
            if (folder.path == folderFile.path) {
                return folder
            }
        }
        val newFolder = LocalMediaFolder()
        newFolder.name = folderFile.name
        newFolder.path = folderFile.absolutePath
        return newFolder
    }

    interface LocalMediaLoadListener {
        fun loadComplete(folders: List<LocalMediaFolder>)
    }

    companion object {
        // load type
        val TYPE_CONTRACT = 0
        val TYPE_IMAGE = 1
        val TYPE_VIDEO = 2

        private val IMAGE_PROJECTION = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DATA,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.DATE_ADDED,
                MediaStore.Images.Media._ID)

        private val VIDEO_PROJECTION = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Video.Media.DATA,
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.DATE_ADDED,
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DURATION)
    }


}
