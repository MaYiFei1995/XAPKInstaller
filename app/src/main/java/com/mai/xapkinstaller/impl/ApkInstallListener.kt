package com.mai.xapkinstaller.impl

interface ApkInstallListener{
    /**
     * 新增一个package
     *
     * @param pkg 新增的包名
     */
    fun onPackageAdded(pkg: String){}

    /**
     * 删除了一个package
     *
     * @param pkg 移除的包名
     */
    fun onPackageDeleted(pkg: String){}
}