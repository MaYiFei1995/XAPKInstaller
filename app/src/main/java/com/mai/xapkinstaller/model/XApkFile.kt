package com.mai.xapkinstaller.model

import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.text.format.Formatter
import android.util.Log
import com.mai.xapkinstaller.App.Companion.app
import org.json.JSONObject
import java.io.File
import java.io.InputStreamReader
import java.util.zip.ZipFile
import com.mai.xapkinstaller.R

class XApkFile(var file: File) {

    companion object {
        /**
         * 根据文件名的后缀判断文件类型
         */
        fun getType(fileName: String): TYPE {
            val lowerName = fileName.toLowerCase()
            return if (lowerName.endsWith(".xapk")) TYPE.XAPK else if (lowerName.endsWith(".apk")) TYPE.APK else TYPE.UNKNOWN
        }

        /**
         * 文件类型
         * XAPK文件
         * APK文件
         * 其他文件
         */
        enum class TYPE {
            APK, XAPK, UNKNOWN
        }
    }

    /**
     * 文件类型，默认未知
     */
    var type = TYPE.UNKNOWN

    /**
     * 文件名
     */
    var fileName: String = file.name

    /**
     * 应用的ICON
     */
    var icon: Drawable? = null

    /**
     * 应用名
     */
    var appName = ""

    /**
     * 文件大小
     */
    var size = Formatter.formatFileSize(app, file.length())!!

    /**
     * APP的最低版本Int值，默认为-1
     */
    var minSDKVersion = -1

    /**
     * APP的版本名,默认为 "-1"
     */
    private var versionName = "-1"

    /**
     * APP的内部版本号，默认为-1
     */
    private var versionCode = -1

    /**
     * ApplicationId
     */
    var packageName = ""

    init {
        try {
            //获取type
            type = getType(file.name)
            when (type) {
                TYPE.XAPK -> {
                    //处理zip文件，解析manifest文件获取应用信息
                    val zFile = ZipFile(file)
                    for (entry in zFile.entries()) {
                        if (!entry.isDirectory && entry.name == "manifest.json") {
                            val jsonObject = JSONObject(InputStreamReader(zFile.getInputStream(entry)).readText())
                            appName = jsonObject.getString("name")
                            minSDKVersion = jsonObject.getString("min_sdk_version").toInt()
                            packageName = jsonObject.getString("package_name")
                            versionName = jsonObject.getString("version_name")
                            versionCode = jsonObject.getString("version_code").toInt()
                            continue
                        }
                        if (!entry.isDirectory && entry.name == "icon.png" && icon == null) {
                            icon = BitmapDrawable(app.resources, BitmapFactory.decodeStream(zFile.getInputStream(entry)))
                        }
                    }
                }
                TYPE.APK -> {
                    //通过PM获取apk文件的应用信息
                    val pm = app.packageManager
                    val packageInfo = pm.getPackageArchiveInfo(file.absolutePath, PackageManager.GET_ACTIVITIES)
                    if (packageInfo != null) {
                        val appInfo = packageInfo.applicationInfo
                        appInfo.sourceDir = file.absolutePath
                        appInfo.publicSourceDir = file.absolutePath
                        //获取应用名
                        appName = pm.getApplicationLabel(appInfo).toString()
                        //获取包名
                        packageName = appInfo.packageName
                        //获取图标
                        icon = pm.getApplicationIcon(appInfo)
                        //获取最低版本
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            minSDKVersion = appInfo.minSdkVersion
                        }
                        //获取应用的版本信息
                        versionName = packageInfo.versionName
                        versionCode = packageInfo.versionCode
                    }
                }
                else -> {
                    //打印错误信息
                    Log.e("XApkFile", "${file.name} type is unknown!")
                }
            }
            Log.i("XApkFile", toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    /**
     * @return 返回应用文件的详情字符串
     */
    fun toDetailString(): String {
        return "${String.format(app.getString(R.string.detail_file), file)}\n" +
                "${String.format(app.getString(R.string.detail_size), size)}\n" +
                "${String.format(app.getString(R.string.detail_appName), appName)}\n" +
                "${String.format(app.getString(R.string.detail_versionName), versionName)}\n" +
                "${String.format(app.getString(R.string.detail_versionCode), versionCode)}\n" +
                "${String.format(app.getString(R.string.detail_packageName), packageName)}\n" +
                String.format(app.getString(R.string.detail_minSDKVersion), minSDKVersion)
    }

    override fun toString(): String {
        return "File: $file, type: $type, fileName: $fileName, appName: $appName, size: $size, minSDKVersion: $minSDKVersion, versionName: $versionName, versionCode: $versionCode, packageName: $packageName"
    }
}