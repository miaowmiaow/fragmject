package com.example.fragment.library.base.utils

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import java.util.*
import kotlin.collections.HashMap

object ActivityResultHelper {

    /**
     * 忽略电池优化,保持后台常驻
     */
    @SuppressLint("BatteryLife")
    fun FragmentActivity.requestIgnoreBatteryOptimizations() {
        //申请加入白名单
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                return
            }
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            //判断应用是否在白名单中
            if (powerManager.isIgnoringBatteryOptimizations(packageName)) {
                return
            }
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
            intent.data = Uri.parse("package:${packageName}")
            startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 申请日历权限
     *
     * @param callback 回调
     */
    fun FragmentActivity.requestCalendar(callback: PermissionsCallback? = null) {
        val permissions = arrayOf(
            Manifest.permission.READ_CALENDAR,
            Manifest.permission.WRITE_CALENDAR
        )
        requestPermissions(permissions, callback)
    }

    /**
     * 申请相机权限
     *
     * @param callback 回调
     */
    fun FragmentActivity.requestCamera(callback: PermissionsCallback? = null) {
        val permissions = arrayOf(
            Manifest.permission.CAMERA
        )
        requestPermissions(permissions, callback)
    }

    /**
     * 申请联系人权限
     *
     * @param callback 回调
     */
    fun FragmentActivity.requestContacts(callback: PermissionsCallback? = null) {
        val permissions = arrayOf(
            Manifest.permission.GET_ACCOUNTS,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.WRITE_CONTACTS
        )
        requestPermissions(permissions, callback)
    }

    /**
     * 申请位置权限
     *
     * @param callback 回调
     */
    fun FragmentActivity.requestLocation(callback: PermissionsCallback? = null) {
        val permissions = arrayOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        requestPermissions(permissions, callback)
    }

    /**
     * 申请电话权限
     *
     * @param callback 回调
     */
    fun FragmentActivity.requestPhone(callback: PermissionsCallback? = null) {
        val permissions = arrayOf(
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_CALL_LOG,
        )
        requestPermissions(permissions, callback)
    }

    /**
     * 申请录音权限
     *
     * @param callback 回调
     */
    fun FragmentActivity.requestRecordAudio(callback: PermissionsCallback? = null) {
        val permissions = arrayOf(
            Manifest.permission.RECORD_AUDIO
        )
        requestPermissions(permissions, callback)
    }

    /**
     * 申请传感器权限
     *
     * @param callback 回调
     */
    @RequiresApi(Build.VERSION_CODES.KITKAT_WATCH)
    fun FragmentActivity.requestSensors(callback: PermissionsCallback? = null) {
        val permissions = arrayOf(
            Manifest.permission.BODY_SENSORS
        )
        requestPermissions(permissions, callback)
    }

    /**
     * 申请短信权限
     *
     * @param callback 回调
     */
    fun FragmentActivity.requestSMS(callback: PermissionsCallback? = null) {
        val permissions = arrayOf(
            Manifest.permission.READ_SMS,
            Manifest.permission.RECEIVE_MMS,
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.SEND_SMS
        )
        requestPermissions(permissions, callback)
    }

    /**
     * 申请存储空间权限
     *
     * @param callback 回调
     */
    fun FragmentActivity.requestStorage(callback: PermissionsCallback? = null) {
        val permissions = arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        requestPermissions(permissions, callback)
    }

    fun FragmentActivity.startForResult(intent: Intent, callback: ActivityCallback) {
        getResultFragment().startForResult(intent, callback)
    }

    fun FragmentActivity.requestPermissions(
        permissions: Array<String>,
        callback: PermissionsCallback?
    ) {
        getResultFragment().requestForPermissions(permissions, callback)
    }

    private fun FragmentActivity.getResultFragment(): ResultFragment {
        val tag = ResultFragment::class.java.simpleName
        var fragment = supportFragmentManager.findFragmentByTag(tag)
        if (fragment == null) {
            fragment = ResultFragment.newInstance()
            val fragmentTransaction = supportFragmentManager.beginTransaction()
            fragmentTransaction.add(fragment, tag)
            fragmentTransaction.commitAllowingStateLoss()
            supportFragmentManager.executePendingTransactions()
        }
        return fragment as ResultFragment
    }

}

interface ActivityCallback {
    fun onActivityResult(resultCode: Int, data: Intent?)
}

interface PermissionsCallback {
    fun allow()
    fun deny()
//    fun denyAndNotAskAgain()
}

class ResultFragment : Fragment() {

    companion object {
        @JvmStatic
        fun newInstance(): ResultFragment {
            return ResultFragment()
        }
    }

    private val activityCallbacks: MutableMap<Int, ActivityCallback?> = HashMap()
    private val permissionsCallbacks: MutableMap<Int, PermissionsCallback?> = HashMap()

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val callback: ActivityCallback? = activityCallbacks[requestCode]
        callback?.onActivityResult(resultCode, data)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionsCallbacks[requestCode]?.apply {
            val length: Int = grantResults.size
            for (i in 0 until length) {
                if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
//                if (ActivityCompat.shouldShowRequestPermissionRationale(
//                        requireContext(),
//                        permissions[i]
//                    )
//                ) {
//                    callback?.denyAndNotAskAgain()
//                } else {
//                    callback?.deny()
//                }
                    deny()
                    return
                }
            }
            if (length > 0) {
                allow()
            }
        }
    }

    fun startForResult(intent: Intent, callback: ActivityCallback?) {
        val requestCode: Int = Random().nextInt(0x0000FFFF)
        activityCallbacks[requestCode] = callback
        startActivityForResult(intent, requestCode)
    }

    /**
     * 动态权限申请方法
     */
    fun requestForPermissions(permissions: Array<String>, callback: PermissionsCallback?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            for (permission in permissions) {
                val result = ActivityCompat.checkSelfPermission(requireContext(), permission)
                if (result == PackageManager.PERMISSION_DENIED) {
                    val requestCode = Random().nextInt(0x0000FFFF)
                    permissionsCallbacks[requestCode] = callback
                    requestPermissions(permissions, requestCode)
                    return
                }
            }
        }
        callback?.allow()
    }

}