package com.mai.xapkinstaller.util

import android.app.AlertDialog
import android.os.AsyncTask
import android.os.Environment
import com.mai.xapkinstaller.MainActivity.Companion.baseDirStr
import com.mai.xapkinstaller.MainActivity.Companion.isWorking
import com.mai.xapkinstaller.MainActivity.Companion.mActivity
import com.mai.xapkinstaller.R
import com.mai.xapkinstaller.model.XApkFile
import org.json.JSONObject
import java.io.File

class Core(xApkFile: XApkFile) : AsyncTask<String, String, String>() {

    private val FLAG_OK = "200"
    private val FLAG_ERROR = "77777"

    private val mXApkFile = xApkFile
    private val mFile = xApkFile.file

    /**
     * 工作开始前
     */
    override fun onPreExecute() {
        super.onPreExecute()
        //显示加载页面
        mActivity.showLoadingView(true)
        //更改工作状态
        isWorking = true
    }

    /**
     * 后台的耗时工作
     */
    override fun doInBackground(vararg params: String?): String {
        mActivity.runOnUiThread { mActivity.showLoadingView(true, resId = R.string.cleaning) }
        //删除缓存目录
        XApkTool.deleteDir(baseDirStr)
        //创建缓存目录
        val rootDirFile = File(baseDirStr)
        if (!rootDirFile.exists())
            rootDirFile.mkdir()

        mActivity.runOnUiThread { mActivity.showLoadingView(true, resId = R.string.unziping) }

        //解压XApk文件
        var b = XApkTool.unZip(mFile.toString(), rootDirFile.toString())
        if (b) {
            //复制数据包文件
            mActivity.runOnUiThread { mActivity.showLoadingView(true, resId = R.string.copying) }
            b = try {
                copyDir(File("$baseDirStr/Android"), File("${Environment.getExternalStorageDirectory().path}/Android"))
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
        return if (b) FLAG_OK else FLAG_ERROR
    }

    /**
     * 工作结束
     */
    override fun onPostExecute(result: String) {
        super.onPostExecute(result)
        mActivity.showLoadingView(true, resId = R.string.reading)
        //查找应用名和apk文件的结果：1->成功; 0->失败
        var mode: Int
        //应用名
        var name = "Unknown"
        //apk文件
        var apk = ""
        //是否成功
        if (result == FLAG_OK) {
            try {
                //获取应用名
                name = JSONObject(readTextFile("$baseDirStr/manifest.json")).getString("name")
                //获取apk文件
                val n = File(baseDirStr)
                for (apkFile in n.listFiles()) {
                    if (apkFile.name.toLowerCase().endsWith(".apk")) {
                        apk = apkFile.toString()
                        break
                    }
                }
                mode = 1
            } catch (e: Exception) {
                mode = 0
            }
        } else {
            mode = 0
        }

        //隐藏加载界面
        mActivity.showLoadingView(false)


        //展示结果的对话框
        val builder = AlertDialog.Builder(mActivity)
        builder.setMessage(
                if (mode == 1)
                    String.format(mActivity.getString(R.string.install_message), name)
                else
                    mActivity.getString(R.string.get_name_or_apk_file_error)
        )
        if (mode == 1)
            builder.setPositiveButton(mActivity.getString(R.string.install)) { _, _ ->
                //拉起APK安装并删掉解压的缓存目录中的文件
                XApkTool.openApk(apk, true)
            }
        else
            builder.setNegativeButton(mActivity.getString(android.R.string.ok), null)

        builder.create()
                .show()

        isWorking = false
    }


    /**
     * 以text的方式读取文件
     *
     * @param path 文件的目录
     */
    private fun readTextFile(path: String): String {
        return File(path).readText()
    }

    /**
     * 复制目录
     *
     * @param source 源目录
     * @param target 目标目录
     */
    private fun copyDir(source: File, target: File) {
        val pkgObbDir = File("$target/obb/${mXApkFile.packageName}")
        if (pkgObbDir.exists()) {
            val tempTarget = File("$pkgObbDir${System.currentTimeMillis()}")
            pkgObbDir.renameTo(tempTarget)
            XApkTool.deleteDir(tempTarget.toString())
        }

        source.copyRecursively(target)
    }

}