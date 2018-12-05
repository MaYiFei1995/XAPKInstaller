package com.mai.xapkinstaller.util

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.support.v4.content.FileProvider
import com.mai.xapkinstaller.BuildConfig
import com.mai.xapkinstaller.MainActivity
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipFile

object XApkTool {
    /**
     * 解压zip文件
     *
     * @param zipFile 待解压的文件路径
     * @param targetPath 释放的目标路径
     *
     * @return 是否成功解压

     */
    fun unZip(zipFile: String, targetPath: String): Boolean {

        if (zipFile.isEmpty()) {
            return false
        }

        try {
            //临时文件
            val temp = File(targetPath)
            //为临时文件创建目录
            temp.mkdir()
            System.out.println("$targetPath created")
            //将源XAPK文件识别为ZipFile
            val zFile = ZipFile(File(zipFile))
            //遍历zipFile中的entry
            for (entry in zFile.entries()) {
                //根据entry的名字创建新的文件
                val destinationFilePath = File(targetPath, entry.name)
                //创建远程的entry的父级目录
                destinationFilePath.parentFile.mkdirs()
                if (!entry.isDirectory) {
                    //buffer输入流
                    val bis = BufferedInputStream(zFile.getInputStream(entry))
                    //buffer输出流
                    val bos = BufferedOutputStream(FileOutputStream(destinationFilePath), 1024)


                    //fun1 use lambda
/*                    val buffer = ByteArray('Ѐ'.toInt())
                    var b = 0
                    while ({b = bis.read(buffer, 0, 1024); b}() != -1){
                        bos.write(buffer, 0, b)
                    }*/

                    //fun2 use also
/*                    //写入，这里also == { block(this); return this; }
                    var read = -1
                    bis.use { _ ->
                        bos.use {
                            while (bis.read().also { read = it } != -1) {
                                it.write(read)
                            }
                        }
                    }*/

                    //fun3 use Kotlin.copyTo
                    bis.copyTo(bos)
                    bos.flush()
                    bos.close()
                    bis.close()
                }
            }
        } catch (e: Exception) {
            System.out.println("IOError :$e")
            return false
        }
        return true
    }

    /**
     * 删除目录
     *
     * @param path 要删除的目录
     * @return 是否成功删除
     */
    fun deleteDir(path: String): Boolean {
        val dir = File(path)
        //如果目录已存在，删除目录
        if (dir.exists()) {
            val files = dir.listFiles() ?: return true
            for (i in files.indices) {
                if (files[i].isDirectory) {
                    deleteDir(files[i].absolutePath)
                } else {
                    files[i].delete()
                }
            }
        }
        return dir.delete()
    }

    /**
     * 删除单个文件
     *
     * @param path 待删除的文件的路径
     * @return 是否成功删除
     */
    fun deleteFile(path: String): Boolean {
        return File(path).delete()
    }

    /**
     * 打开Apk文件
     *
     * @param apk apk的路径
     * @param isXAPK 是否为XAPK，如果是的话，删除缓存目录，默认为false
     */
    fun openApk(apk: String, isXAPK: Boolean = false) {
        //待安装的apkFile
        val apkFile = File(apk)
        val intent = Intent(Intent.ACTION_VIEW)
        //大于等于7.0版本，需要通过FileProvider设置意图
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            intent.setDataAndType(FileProvider.getUriForFile(MainActivity.mActivity, BuildConfig.APPLICATION_ID + ".provider", apkFile), "application/vnd.android.package-archive")
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } else {
            intent.setDataAndType(Uri.fromFile(apkFile), "application/vnd.android.package-archive")
        }
        //拉起安装
        MainActivity.mActivity.startActivity(intent)
        //删除xapk的缓存文件
        if (isXAPK) {
            XApkTool.deleteDir("${MainActivity.baseDirStr}/Android")
            XApkTool.deleteFile("${MainActivity.baseDirStr}/manifest.json")
            XApkTool.deleteFile("${MainActivity.baseDirStr}/icon.png")
        }
    }
}