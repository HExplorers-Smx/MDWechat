package com.blanke.mdwechat.util

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.blanke.mdwechat.BuildConfig
import com.blanke.mdwechat.Common
import java.io.File

/**
 * Centralizes the shared-storage policy used by MDWechat.
 *
 * This module intentionally keeps its public workspace at /sdcard/mdwechat instead of moving
 * to an app-private directory: the Xposed hook runs inside WeChat's process and still needs
 * file-path based access to configs, icons and logs.
 */
object StorageAccess {
    const val REQUEST_LEGACY_EXTERNAL_STORAGE = 0x4D57
    const val REQUEST_MANAGE_EXTERNAL_STORAGE = 0x4D58

    fun appDirectoryName(): String {
        return if (Common.isVXPEnv && BuildConfig.DEBUG) Common.APP_VXP_DIR else Common.APP_DIR
    }

    fun appDirectoryPath(): String {
        return File(Environment.getExternalStorageDirectory(), appDirectoryName()).absolutePath + File.separator
    }

    fun appDirectory(): File = File(appDirectoryPath())

    fun hasSharedStorageAccess(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun requestSharedStorageAccess(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            requestAllFilesAccess(activity)
        } else {
            ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ),
                    REQUEST_LEGACY_EXTERNAL_STORAGE
            )
        }
    }

    private fun requestAllFilesAccess(activity: Activity) {
        val packageUri = Uri.parse("package:${activity.packageName}")
        val appSpecificIntent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, packageUri)
        val fallbackIntent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
        try {
            activity.startActivityForResult(appSpecificIntent, REQUEST_MANAGE_EXTERNAL_STORAGE)
        } catch (e: ActivityNotFoundException) {
            activity.startActivityForResult(fallbackIntent, REQUEST_MANAGE_EXTERNAL_STORAGE)
        }
    }

    fun ensureBaseDirectories(): Boolean {
        val root = appDirectory()
        return root.exists() || root.mkdirs()
    }
}
