package com.blanke.mdwechat.settings

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.widget.Toast
import com.blanke.mdwechat.Common
import com.blanke.mdwechat.R
import com.blanke.mdwechat.config.AppCustomConfig
import com.blanke.mdwechat.util.FileUtils
import com.blanke.mdwechat.util.StorageAccess
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import kotlin.concurrent.thread


/**
 * Created by blanke on 2017/6/8.
 */

class SettingsActivity : Activity() {
    private lateinit var fab: View
    private var waitingForStorageGrant = false
    private var hasStartedConfigCopy = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        Common.APP_DIR_PATH
        fab = findViewById(R.id.fab)
        fab.setOnClickListener {
            copyConfig()
            goToWechatSettingPage()
        }
        verifyStoragePermissions(this)
    }

    override fun onResume() {
        super.onResume()
        if (waitingForStorageGrant) {
            waitingForStorageGrant = false
            if (StorageAccess.hasSharedStorageAccess(this)) {
                copyConfig()
            } else {
                Toast.makeText(this, R.string.msg_permission_fail, Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    private fun showSettingsFragment() {
        findViewById<View>(R.id.pb_loading).visibility = View.GONE
        fab.visibility = View.VISIBLE
        fragmentManager.beginTransaction().replace(R.id.setting_fl_container,
                SettingsFragment()).commit()
    }

    private fun copySharedPrefences() {
        val sharedPrefsDir = File(filesDir, "../shared_prefs")
        val sharedPrefsFile = File(sharedPrefsDir, Common.MOD_PREFS + ".xml")
        val sdSPFile = File(AppCustomConfig.getConfigFile(Common.MOD_PREFS + ".xml"))
        if (sharedPrefsFile.exists()) {
            sdSPFile.parentFile?.mkdirs()
            FileOutputStream(sdSPFile).use { outStream ->
                FileInputStream(sharedPrefsFile).use { input ->
                    FileUtils.copyFile(input, outStream)
                }
            }
        } else if (sdSPFile.exists()) { // restore sharedPrefsFile
            sharedPrefsFile.parentFile?.mkdirs()
            FileInputStream(sdSPFile).use { input ->
                FileOutputStream(sharedPrefsFile).use { outStream ->
                    FileUtils.copyFile(input, outStream)
                }
            }
        }
    }

    private fun goToWechatSettingPage() {
        Toast.makeText(this, R.string.msg_kill_wechat, Toast.LENGTH_SHORT).show()
        val intent = Intent(Settings.ACTION_DATA_ROAMING_SETTINGS)
        intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        intent.data = Uri.fromParts("package", Common.WECHAT_PACKAGENAME, null)
        startActivity(intent)
    }

    private fun copyConfig() {
        if (hasStartedConfigCopy) {
            return
        }
        hasStartedConfigCopy = true
        thread {
            StorageAccess.ensureBaseDirectories()
            FileUtils.copyAssets(this, Common.APP_DIR_PATH, Common.CONFIG_WECHAT_DIR)
            FileUtils.copyAssets(this, Common.APP_DIR_PATH, Common.CONFIG_VIEW_DIR)
            FileUtils.copyAssets(this, Common.APP_DIR_PATH, Common.ICON_DIR)
            copySharedPrefences()
            Handler(Looper.getMainLooper()).post {
                showSettingsFragment()
            }
        }
    }

    private fun verifyStoragePermissions(activity: Activity) {
        try {
            if (StorageAccess.hasSharedStorageAccess(activity)) {
                copyConfig()
            } else {
                Toast.makeText(activity, R.string.msg_storage_permission_required, Toast.LENGTH_LONG).show()
                waitingForStorageGrant = true
                StorageAccess.requestSharedStorageAccess(activity)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(activity, R.string.msg_permission_fail, Toast.LENGTH_LONG).show()
            finish()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == StorageAccess.REQUEST_MANAGE_EXTERNAL_STORAGE && !waitingForStorageGrant) {
            verifyStoragePermissions(this)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == StorageAccess.REQUEST_LEGACY_EXTERNAL_STORAGE) {
            if (StorageAccess.hasSharedStorageAccess(this)) {
                copyConfig()
            } else {
                Toast.makeText(this, R.string.msg_permission_fail, Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }
}
