package com.mai.xapkinstaller

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.*
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v4.os.EnvironmentCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.text.format.Formatter
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import com.mai.xapkinstaller.App.Companion.app
import com.nononsenseapps.filepicker.Utils
import com.mai.xapkinstaller.adapter.XApkAdapter
import com.mai.xapkinstaller.impl.ApkInstallListener
import com.mai.xapkinstaller.model.XApkFile
import com.mai.xapkinstaller.util.Core
import com.mai.xapkinstaller.util.PathTool
import com.mai.xapkinstaller.util.XApkTool
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.*
import java.io.File

class MainActivity : AppCompatActivity() {

    companion object {
        lateinit var mActivity: MainActivity
        //base目录
        val baseDirStr = "${Environment.getExternalStorageDirectory().path}/XAPK_Installer"
        //是否正在工作
        var isWorking: Boolean = false
        //缓存的XAPK文件列表
        private var xApkFileList = ArrayList<XApkFile>()

        //系统安装程序的回调
        var installListener = object : ApkInstallListener {
            override fun onPackageAdded(pkg: String) {
                try {
                    for (xApkFile in xApkFileList) {
                        if (xApkFile.packageName == pkg) {
                            //提示安装成功
                            app.toast(String.format(app.getString(R.string.xapk_installed), xApkFile.appName))
                            //删除APK
                            if (xApkFile.type == XApkFile.Companion.TYPE.XAPK)
                                XApkTool.deleteDir(baseDirStr)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        //
        var coreInstance: Core? = null
    }

    enum class ShowType {
        TOAST, DIALOG
    }

    private val tag = "xApkInstaller"
    private val codeRequestPermissions = 9527
    private val codeBrowseFile = 41671

    private var initThread: Thread? = null
    private lateinit var adapter: XApkAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mActivity = this
    }

    override fun onResume() {
        super.onResume()
        if (initThread == null) {
            initThread = Thread(Runnable {
                //第一次onResume
                if (!::adapter.isInitialized) {
                    //检查读写权限
                    if (checkPermissions(true))
                        init()
                    else
                        try {
                            mActivity.runOnUiThread {
                                showPermissionsError(ShowType.DIALOG)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                } else
                    init()
            })
            initThread!!.start()
        }
    }

    /**
     * 提示权限错误
     *
     * @param type Toast/AlertDialog
     */
    private fun showPermissionsError(type: ShowType) {
        when (type) {
            ShowType.TOAST -> {
                longToast(R.string.no_permission_alert)
            }
            ShowType.DIALOG -> {
                initThread?.interrupt()
                alert(R.string.no_permission_alert).show()
            }
        }
    }

    /**
     * 开始查找本地文件
     */
    private fun init() {
        Log.i(tag, "Init...")
        showLoadingView(true, getString(R.string.initlizing))

        when (intent.action) {
            Intent.ACTION_SEND, Intent.ACTION_VIEW -> {
                try {
                    //获取文件的Uri
                    val mUri = if (intent.action == Intent.ACTION_SEND) intent.getParcelableExtra<Parcelable>(Intent.EXTRA_STREAM) as Uri? else intent.data
                    if (mUri != null) {
                        //根据Uri获取文件的路径
                        val mPath = PathTool.getPath(this, mUri)
                        try {
                            val xApkFile = XApkFile(File(mPath))
                            if (xApkFile.type != XApkFile.Companion.TYPE.UNKNOWN) {
                                alert(String.format(getString(R.string.installing), xApkFile.appName)) {
                                    noButton { }
                                    yesButton { prepareWork(xApkFile) }
                                }.show()
                            } else {
                                toast(R.string.unknown_xapk_file)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            toast(R.string.unknown_error)
                            showLoadingView(false)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    toast(R.string.unknown_error)
                    showLoadingView(false)

                }
            }
        }

        initXApkFiles()
    }

    /**
     * 初始化xapk文件信息
     */
    private fun initXApkFiles() {
        val path = ArrayList<String>()
        try {
            val resultDirs = getExternalStorageDirectories()
            if (resultDirs.isNotEmpty()) {
                val dir = resultDirs[0]
                if (dir.isNotEmpty())
                    path.add(dir)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        path.add(Environment.getExternalStorageDirectory().absolutePath)

        val tempList = ArrayList<XApkFile>()

        //获取所有的XApk文件信息
        for (i in 0 until path.size) {
            addXApkFiles(File(path[i]), tempList)
        }

        //新增的文件不为空，清空替换
        if (tempList.isNotEmpty()) {
            xApkFileList.clear()
            xApkFileList.addAll(tempList)
        }

        //适配器未初始化则初始化RecyclerView，已初始化则更新适配器的数据集，用于menu的refresh点击事件
        if (!::adapter.isInitialized) {
            mActivity.runOnUiThread {
                initRecyclerView()
            }
        } else {
            mActivity.runOnUiThread {
                adapter.notifyDataSetChanged()
                showLoadingView(false)
            }
        }
    }

    /**
     * 根据目录初始化XApk文件
     */
    private fun addXApkFiles(dir: File, tempList: ArrayList<XApkFile>) {
        try {
            val listFile = dir.listFiles()
            if (listFile.isNotEmpty()) {
                for (i in 0 until listFile.size) {
                    if (listFile[i].isDirectory) {
                        addXApkFiles(listFile[i], tempList)
                    } else {
                        if (XApkFile.getType(listFile[i].name) != XApkFile.Companion.TYPE.UNKNOWN) {
                            tempList.add(XApkFile(listFile[i]))
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 初始化文件列表
     */
    private fun initRecyclerView() {
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = XApkAdapter(xApkFileList,
                object : XApkAdapter.OnChildClickListener {
                    override fun onInstall(holder: XApkAdapter.XApkViewHolder, position: Int) {
                        try {
                            prepareWork(xApkFileList[position])
                        } catch (e: Exception) {
                            toast(R.string.unknown_error)
                        }
                    }

                    override fun onDelete(holder: XApkAdapter.XApkViewHolder, position: Int) {
                        alert(String.format(getString(R.string.delete_confirm), xApkFileList[position].fileName)) {
                            noButton { }
                            okButton {
                                //删除文件并更新列表
                                if (XApkTool.deleteFile(xApkFileList[position].file.absolutePath)) {
                                    xApkFileList.removeAt(position)
                                    adapter.notifyDataSetChanged()
                                }
                            }
                        }.show()
                    }
                },
                object : XApkAdapter.OnLongClickListener {
                    override fun onLongClick(position: Int) {
                        alert(xApkFileList[position].toDetailString(), getString(R.string.detail)).show()
                    }
                })
        recyclerView.adapter = adapter

        showLoadingView(false)
    }

    /**
     * 重新扫描本地文件
     */
    private fun refreshData() {
        if (initThread != null) {
            initThread!!.interrupt()
            initThread = null
        }
        initThread = Thread(Runnable {
            init()
        })
        initThread!!.start()
    }

    /**
     * 获取外部存储目录
     */
    private fun getExternalStorageDirectories(): ArrayList<String> {
        //SDCARD
        val result = ArrayList<String>()
        //SDK>=4.4
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            // Returns absolute paths to application-specific directories on all
            // shared/external storage devices where the application can place
            // persistent files it owns
            val externalDirs = getExternalFilesDirs(null)
            for (file in externalDirs) {
                val path = file.path.split("/Android")[0]
                if (
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                        //大于5.0时使用目录是否可卸载判断
                            Environment.isExternalStorageRemovable(file)
                        else {
                            //小于5.0时用判断是否可读写
                            Environment.MEDIA_MOUNTED == EnvironmentCompat.getStorageState(file)
                        }
                )
                    result.add(path)
            }
        }

        //从结果中去除可能不是外部存储的目录，比如OTG
        //大于等于6.0
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            var i = 0
            while (i < result.size) {
                if (!result[i].toLowerCase().matches(Regex(".*[0-9a-f]{4}[-][0-9a-f]{4}"))) {
                    result.removeAt(i--)
                }
                i++
            }
        } else {
            var i = 0
            while (i < result.size) {
                if (!result[i].toLowerCase().contains("ext") && !result[i].toLowerCase().contains("sdcard")) {
                    result.removeAt(i--)
                }
                i++
            }
        }

        return result
    }

    /**
     * 控制加载界面是否显示
     *
     * @param b 是否显示
     * @param str 显示的提示语
     */
    fun showLoadingView(b: Boolean, str: String = getString(R.string.loading), resId: Int = 0) {
        mActivity.runOnUiThread {
            loadingBg.visibility = if (b) View.VISIBLE else View.GONE
            progressLayout.visibility = if (b) View.VISIBLE else View.GONE
            if (b) {
                if (resId != 0)
                    progressMsg.text = getString(resId)
                else
                    progressMsg.text = str
            } else
                progressMsg.text = ""
        }
    }

    /**
     * 检查权限
     *
     * @param b 是否在检查的同时请求权限
     * @return 是否已有读写存储权限
     */
    private fun checkPermissions(b: Boolean): Boolean {
        //读写权限都需要
        return if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if (b) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE), codeRequestPermissions)
            }
            false
        } else {
            true
        }
    }

    /**
     * 请求权限的回调
     */
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            //请求读写权限
            codeRequestPermissions -> {
                //已获得权限，查找文件
                if (checkPermissions(false)) {
                    Log.i(tag, getString(R.string.scanning))
                    initThread = Thread(Runnable {
                        init()
                    })
                    initThread!!.start()
                } else {
                    //无权限，提示错误
                    showPermissionsError(ShowType.DIALOG)
                }
            }
        }
    }

    private fun prepareWork(xApkFile: XApkFile, ignoreSdkVersion: Boolean = false) {
        //检查版本
        if (Build.VERSION.SDK_INT >= xApkFile.minSDKVersion || ignoreSdkVersion) {
            //检查是否有两倍的剩余空间
            if (Environment.getExternalStorageDirectory().freeSpace > xApkFile.file.length() * 2) {
                //检查是否已安装
                if (!isPackageExist(xApkFile.packageName)) {
                    startWork(xApkFile)
                } else {
                    alert(R.string.package_already_exist) {
                        noButton { }
                        positiveButton(R.string.install_persist) { startWork(xApkFile) }
                    }.show()

                }
            } else {
                alert(String.format(getString(R.string.no_enough_space),
                        Formatter.formatFileSize(app, xApkFile.file.length() * 2),
                        Formatter.formatFileSize(app, Environment.getExternalStorageDirectory().freeSpace)), getString(R.string.error)) {
                    noButton { }
                }.show()
            }
        } else {
            alert(String.format(getString(R.string.sdk_version_error), Build.VERSION.SDK_INT, xApkFile.minSDKVersion), getString(R.string.error)) {
                noButton { }
                positiveButton(R.string.install_persist) {
                    prepareWork(xApkFile, true)
                }
            }.show()
        }
    }

    /**
     * 开始工作
     *
     * @param xApkFile 待安装的XApk文件
     */
    private fun startWork(xApkFile: XApkFile) {
        when {
            xApkFile.type == XApkFile.Companion.TYPE.XAPK -> {
                coreInstance = Core(xApkFile)
                coreInstance?.execute()
            }
            xApkFile.type == XApkFile.Companion.TYPE.APK -> {
                alert(String.format(getString(R.string.install_message), xApkFile.appName)) {
                    noButton {  }
                    positiveButton(R.string.install) {
                        XApkTool.openApk(xApkFile.file.toString())
                    }
                }.show()
            }
            else -> {
                toast(R.string.unknown_xapk_file)
            }
        }
    }

    /**
     * 应用是否已安装
     */
    private fun isPackageExist(packageName: String): Boolean {
        for (info in packageManager.getInstalledPackages(0)) {
            if (info.packageName == packageName)
                return true
        }
        return false
    }


    override fun onBackPressed() {
        if (isWorking || (coreInstance != null && coreInstance?.status == AsyncTask.Status.RUNNING)) {
            alert(R.string.exit_message) {
                neutralPressed(android.R.string.cancel) {}
                negativeButton(R.string.stop_only) {
                    coreInstance?.cancel(true)
                    coreInstance = null
                    showLoadingView(false)
                }
                positiveButton(R.string.exit_stop) {
                    coreInstance?.cancel(true)
                    coreInstance = null
                    showLoadingView(false)
                    finish()
                }
            }.show()
        } else {
            if (initThread != null) {
                initThread!!.interrupt()
                initThread = null
            }
            finish()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (isWorking)
            return super.onOptionsItemSelected(item)
        when (item.itemId) {
            //手动选择文件
            R.id.browse -> {
                startActivityForResult(Intent(this, FilePicker::class.java), codeBrowseFile)
            }
            //重新从磁盘扫描文件
            R.id.refresh -> {
                refreshData()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        if (intent == null)
            return
        when (requestCode) {
            codeBrowseFile -> {
                if (resultCode == Activity.RESULT_OK) {
                    val file: File? = Utils.getFileForUri(Utils.getSelectedFilesFromResult(intent).last())
                    if (file != null) {
                        val xApkFile = XApkFile(file)
                        if (xApkFile.type != XApkFile.Companion.TYPE.UNKNOWN) {
                            alert(String.format(getString(R.string.installing), xApkFile.appName)) {
                                noButton { }
                                okButton { prepareWork(xApkFile) }
                            }.show()
                        } else {
                            toast(R.string.unknown_error)
                        }
                    } else
                        toast(R.string.unknown_error)
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, intent)
    }

}