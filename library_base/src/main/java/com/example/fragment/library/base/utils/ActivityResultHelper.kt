package com.example.fragment.library.base.utils

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.annotation.RequiresApi
import com.example.fragment.library.base.component.activity.BaseActivity
import com.example.fragment.library.base.component.dialog.PermissionDialog

object ActivityResultHelper {
    /**
     * 忽略电池优化,保持后台常驻
     */
    @SuppressLint("BatteryLife")
    fun requestIgnoreBatteryOptimizations(activity: BaseActivity) {
        //申请加入白名单
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                return
            }
            val powerManager = activity.getSystemService(Context.POWER_SERVICE) as PowerManager
            //判断应用是否在白名单中
            if (powerManager.isIgnoringBatteryOptimizations(activity.packageName)) {
                return
            }
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
            intent.data = Uri.parse("package:${activity.packageName}")
            activity.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private var screenRecordData: Intent? = null

    /**
     * 申请录屏权限
     */
    fun requestScreenRecord(activity: BaseActivity, onCallback: (Int, Intent?) -> Unit) {
        if (screenRecordData != null) {
            onCallback.invoke(Activity.RESULT_OK, screenRecordData)
        } else {
            requestRecordAudio(activity) {
                requestStorage(activity, object : PermissionsCallback {
                    override fun allow() {
                        val mediaProjectionManager =
                            activity.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                        activity.startForResult(mediaProjectionManager.createScreenCaptureIntent(),
                            object : ActivityCallback {
                                override fun onActivityResult(resultCode: Int, data: Intent?) {
                                    onCallback.invoke(resultCode, data)
                                    screenRecordData = data
                                }
                            }
                        )
                    }

                    override fun deny() {
                        PermissionDialog.storage(activity)
                    }

                    override fun denyAndNotAskAgain() {
                        PermissionDialog.storage(activity)
                    }
                })
            }
        }
    }

    /**
     * 申请日历权限
     *
     * @param callback 回调
     */
    fun requestCalendar(activity: BaseActivity, callback: PermissionsCallback? = null) {
        val permissions = arrayOf(
            Manifest.permission.READ_CALENDAR,
            Manifest.permission.WRITE_CALENDAR
        )
        requestPermissions(activity, permissions, callback)
    }

    /**
     * 申请相机权限
     *
     * @param callback 回调
     */
    fun requestCamera(activity: BaseActivity, callback: PermissionsCallback? = null) {
        val permissions = arrayOf(
            Manifest.permission.CAMERA
        )
        requestPermissions(activity, permissions, callback)
    }

    /**
     * 申请联系人权限
     *
     * @param callback 回调
     */
    fun requestContacts(activity: BaseActivity, callback: PermissionsCallback? = null) {
        val permissions = arrayOf(
            Manifest.permission.GET_ACCOUNTS,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.WRITE_CONTACTS
        )
        requestPermissions(activity, permissions, callback)
    }

    /**
     * 申请位置权限
     *
     * @param callback 回调
     */
    fun requestLocation(activity: BaseActivity, callback: PermissionsCallback? = null) {
        val permissions = arrayOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        requestPermissions(activity, permissions, callback)
    }

    /**
     * 申请电话权限
     *
     * @param callback 回调
     */
    fun requestPhone(activity: BaseActivity, callback: PermissionsCallback? = null) {
        val permissions = arrayOf(
            Manifest.permission.CALL_PHONE
        )
        requestPermissions(activity, permissions, callback)
    }

    /**
     * 申请录音权限
     *
     * @param callback 回调
     */
    fun requestRecordAudio(activity: BaseActivity, callback: PermissionsCallback? = null) {
        val permissions = arrayOf(
            Manifest.permission.RECORD_AUDIO
        )
        requestPermissions(activity, permissions, callback)
    }

    private fun requestRecordAudio(activity: BaseActivity, onCallback: () -> Unit) {
        requestRecordAudio(activity, object : PermissionsCallback {
            override fun allow() {
                onCallback.invoke()
            }

            override fun deny() {
                onCallback.invoke()
            }

            override fun denyAndNotAskAgain() {
                onCallback.invoke()
            }
        })
    }

    /**
     * 申请传感器权限
     *
     * @param callback 回调
     */
    @RequiresApi(Build.VERSION_CODES.KITKAT_WATCH)
    fun requestSensors(activity: BaseActivity, callback: PermissionsCallback? = null) {
        val permissions = arrayOf(
            Manifest.permission.BODY_SENSORS
        )
        requestPermissions(activity, permissions, callback)
    }

    /**
     * 申请短信权限
     *
     * @param callback 回调
     */
    fun requestSMS(activity: BaseActivity, callback: PermissionsCallback? = null) {
        val permissions = arrayOf(
            Manifest.permission.READ_SMS,
            Manifest.permission.RECEIVE_MMS,
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.SEND_SMS
        )
        requestPermissions(activity, permissions, callback)
    }

    /**
     * 申请存储空间权限
     *
     * @param callback 回调
     */
    fun requestStorage(activity: BaseActivity, callback: PermissionsCallback? = null) {
        val permissions = arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        requestPermissions(activity, permissions, callback)
    }

    fun startForResult(activity: BaseActivity, intent: Intent, callback: ActivityCallback) {
        activity.startForResult(intent, callback)
    }

    private fun requestPermissions(
        activity: BaseActivity,
        permissions: Array<String>,
        callback: PermissionsCallback?
    ) {
        activity.requestForPermissions(permissions, callback)
    }

}

interface ActivityCallback {
    fun onActivityResult(resultCode: Int, data: Intent?)
}

interface PermissionsCallback {
    fun allow()
    fun deny()
    fun denyAndNotAskAgain()
}
