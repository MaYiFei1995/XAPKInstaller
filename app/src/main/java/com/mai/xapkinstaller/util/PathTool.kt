package com.mai.xapkinstaller.util

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore

/**
 * Created by Mai on 27/10/2018.
 */

object PathTool {

    /**
     * 通过Uri获取文件的真实地址
     *
     * @param mContext 上下文
     * @param uri 获取到的文件的Uri
     *
     * @return 文件的路径
     */
    fun getPath(mContext: Context, uri: Uri): String {
        var mUri = uri
        var selection = ""
        val selectionArgs = arrayOf("")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && DocumentsContract.isDocumentUri(mContext.applicationContext, uri)) {
            when {
                isExternalStorageDocument(uri) -> return "${Environment.getExternalStorageDirectory()}/${DocumentsContract.getDocumentId(uri).split(":")[1]}"
                isDownloadsDocument(uri) -> mUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), DocumentsContract.getDocumentId(uri).toLong())
                isMediaDocument(uri) -> {
                    mUri = when (DocumentsContract.getDocumentId(uri).split(":")[0]) {
                        "image" -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                        "video" -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                        "audio" -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                        else -> uri
                    }
                    selection = "_id=?"
                    selectionArgs[0] = DocumentsContract.getDocumentId(mUri).split(":")[1]
                }
            }
        }
        if ("content".equals(mUri.scheme, ignoreCase = true)) {
            val projection = arrayOf(MediaStore.Images.Media.DATA)
            try {
                val cursor = mContext.contentResolver.query(mUri, projection, selection, selectionArgs, null)
                val columnIndex = cursor!!.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                if (cursor.moveToFirst()) {
                    val returnValue = cursor.getString(columnIndex)
                    cursor.close()
                    return returnValue
                }
            } catch (ignore: Exception) {
            }
        } else if ("file".equals(mUri.scheme, ignoreCase = true)){
            return mUri.path
        }
        return ""
    }

    /**
     * @param uri 待检查的文件的Uri
     * @return uri的authority是否为ExternalStorageProvider
     */
    private fun isExternalStorageDocument(uri: Uri): Boolean {
        return "com.android.externalstorage.documents" == uri.authority
    }

    /**
     * @param uri 待检查的文件的Uri
     * @return uri的authority是否为DownloadsProvider.
     */
    private fun isDownloadsDocument(uri: Uri): Boolean {
        return "com.android.providers.downloads.documents" == uri.authority
    }

    /**
     * @param uri 待检查的文件的Uri
     * @return uri的authority是否为MediaProvider.
     */
    private fun isMediaDocument(uri: Uri): Boolean {
        return "com.android.providers.media.documents" == uri.authority
    }

}