package com.yongchun.library.utils;

import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;

import com.yongchun.library.model.LocalMedia;
import com.yongchun.library.model.LocalMediaFolder;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Logger;


public class LocalMediaLoader {
    // load type
    public static final int TYPE_CONTACK = 0;
    public static final int TYPE_IMAGE = 1;
    public static final int TYPE_VIDEO = 2;

    private final static String[] IMAGE_PROJECTION = {
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media._ID};

    private final static String[] VIDEO_PROJECTION = {
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DURATION};

    private int type = TYPE_IMAGE;
    private FragmentActivity activity;

    public LocalMediaLoader(FragmentActivity activity, int type) {
        this.activity = activity;
        this.type = type;
    }

    private HashSet<String> mDirPaths = new HashSet<String>();//文件夹路径

    public void loadAllImage(final LocalMediaLoadListener imageLoadListener) {
        activity.getSupportLoaderManager().initLoader(type, null, new LoaderManager.LoaderCallbacks<Cursor>() {
            @Override
            public Loader<Cursor> onCreateLoader(int id, Bundle args) {
                CursorLoader cursorLoader = null;
                if (id == TYPE_IMAGE) {
                    cursorLoader = new CursorLoader(
                            activity, MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            IMAGE_PROJECTION, MediaStore.Images.Media.MIME_TYPE + "=? or "
                            + MediaStore.Images.Media.MIME_TYPE + "=?",
                            new String[]{"image/jpeg", "image/png"}, IMAGE_PROJECTION[2] + " DESC");
                } else if (id == TYPE_VIDEO) {
                    cursorLoader = new CursorLoader(
                            activity, MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                            VIDEO_PROJECTION, null, null, VIDEO_PROJECTION[2] + " DESC");
                }
                return cursorLoader;
            }

            @Override
            public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
                ArrayList<LocalMediaFolder> imageFolders = new ArrayList<LocalMediaFolder>();//一组文件夹
                LocalMediaFolder allImageFolder = new LocalMediaFolder();//全部图片-文件夹
                List<LocalMedia> allImages = new ArrayList<LocalMedia>();//图片
                //while循环
                while (data != null && data.moveToNext()) {
                    String path = data.getString(data.getColumnIndex(MediaStore.Images.Media.DATA));// 图片的路径
                    allImages.add(new LocalMedia(path));


                    File file = new File(path);
                    if (!file.exists())
                        continue;
                    // 获取该图片的目录路径名
                    File parentFile = file.getParentFile();//图片对应的文件夹
                    if (parentFile == null || !parentFile.exists())
                        continue;

                    String dirPath = parentFile.getAbsolutePath();//文件夹路径

                    // 利用一个HashSet防止多次扫描同一个文件夹
                    if (mDirPaths.contains(dirPath)) {
                        continue;
                    } else {
                        mDirPaths.add(dirPath);
                    }

                    if (parentFile.list() == null)
                        continue;

                    // TODO: 2016/08/19 处理文件夹数据    文件夹与所有图片顺序没有对应
                    //处理文件夹数据
                    LocalMediaFolder localMediaFolder = getImageFolder(path, imageFolders);

                    Log.e("caoyang", "本地文件夹名称——————   " + parentFile.toString());

                    //获取文件夹里的所有图片
                    File[] files = parentFile.listFiles(new FilenameFilter() {
                        @Override
                        public boolean accept(File dir, String filename) {
                            if (filename.endsWith(".jpg") | filename.endsWith(".png") || filename.endsWith(".jpeg"))
                                return true;
                            return false;
                        }
                    });


                    List<LocalMedia> images = new ArrayList<>();

                    for (int i = 0; i < files.length; i++) {
                        //allImages.add(localMedia);
                        images.add(new LocalMedia(files[i].getAbsolutePath()));
                    }
                    if (images.size() > 0) {
                        Collections.sort(images);
                        localMediaFolder.setImages(images);
                        localMediaFolder.setFirstImagePath(images.get(0).getPath());
                        localMediaFolder.setImageNum(localMediaFolder.getImages().size());
                        imageFolders.add(localMediaFolder);
                    }
                }

                allImageFolder.setImages(allImages);
                allImageFolder.setImageNum(allImageFolder.getImages().size());
                if (allImages.size() != 0) {
                    allImageFolder.setFirstImagePath(allImages.get(0).getPath());
                }
                allImageFolder.setName(activity.getString(com.yongchun.library.R.string.all_image));
                imageFolders.add(allImageFolder);
                sortFolder(imageFolders);
                imageLoadListener.loadComplete(imageFolders);
                if (data != null) data.close();
            }

            @Override
            public void onLoaderReset(Loader<Cursor> loader) {
            }
        });
    }

    private void sortFolder(List<LocalMediaFolder> imageFolders) {
        // 文件夹按图片数量排序
        Collections.sort(imageFolders, new Comparator<LocalMediaFolder>() {
            @Override
            public int compare(LocalMediaFolder lhs, LocalMediaFolder rhs) {
                if (lhs.getImages() == null || rhs.getImages() == null) {
                    return 0;
                }
                int lsize = lhs.getImageNum();
                int rsize = rhs.getImageNum();
                return lsize == rsize ? 0 : (lsize < rsize ? 1 : -1);
            }
        });
    }

    private LocalMediaFolder getImageFolder(String path, List<LocalMediaFolder> imageFolders) {
        File imageFile = new File(path);
        File folderFile = imageFile.getParentFile();//图片对应的文件夹
        //搜寻所有文件夹
        for (LocalMediaFolder folder : imageFolders) {
            if (folder.getName().equals(folderFile.getName())) {
                return folder;
            }
        }
        LocalMediaFolder newFolder = new LocalMediaFolder();
        newFolder.setName(folderFile.getName());
        newFolder.setPath(folderFile.getAbsolutePath());
        return newFolder;
    }

    public interface LocalMediaLoadListener {
        void loadComplete(List<LocalMediaFolder> folders);
    }

}
