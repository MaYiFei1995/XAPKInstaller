package com.mai.xapkinstaller

import android.os.Environment
import com.nononsenseapps.filepicker.AbstractFilePickerFragment
import com.nononsenseapps.filepicker.FilePickerActivity
import com.nononsenseapps.filepicker.FilePickerFragment

import java.io.File

/**
 * Created by Mai on 27/10/2018.
 */

class FilePicker : FilePickerActivity() {

    override fun getFragment(startPath: String?, mode: Int, allowMultiple: Boolean, allowCreateDir: Boolean, allowExistingFile: Boolean, singleClick: Boolean): AbstractFilePickerFragment<File> {
        val fragment = CustomFilePickerFragment()
        fragment.setArgs(startPath ?: Environment.getExternalStorageDirectory().path, mode, allowMultiple, allowCreateDir, allowExistingFile, singleClick)
        return fragment
    }

    /**
     * 自定义的文件选择Fragment
     */
    class CustomFilePickerFragment : FilePickerFragment() {

        /**
         * 根据文件的后缀名判断是否为xapk文件
         */
        private fun isFileXApk(file: File): Boolean {
            val path = file.path
            val i = path.lastIndexOf(".")
            return if (i < 0) false else path.substring(i).toLowerCase() == ".xapk"
        }

        override fun isItemVisible(file: File): Boolean {
            val isItemVisible = super.isItemVisible(file)
            if (isItemVisible && !isDir(file) && (mode == AbstractFilePickerFragment.MODE_FILE || mode == AbstractFilePickerFragment.MODE_FILE_AND_DIR)) {
                return isFileXApk(file)
            }
            return isItemVisible
        }
    }

}