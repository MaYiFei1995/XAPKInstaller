package com.mai.xapkinstaller

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ApkInstallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_PACKAGE_ADDED -> {
                try {
                    MainActivity.installListener.onPackageAdded(intent.dataString.substring(8))
                } catch (ignore: Exception) {
                }
            }
            Intent.ACTION_PACKAGE_REMOVED -> {
                try {
                    MainActivity.installListener.onPackageDeleted(intent.dataString.substring(8))
                } catch (ignore: Exception) {
                }
            }
        }
    }
}