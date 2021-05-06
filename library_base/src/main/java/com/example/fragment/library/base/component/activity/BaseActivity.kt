package com.example.fragment.library.base.component.activity

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.fragment.library.base.utils.ActivityResult
import com.example.fragment.library.base.component.dialog.FullDialog
import java.util.*
import kotlin.collections.HashMap

abstract class BaseActivity : AppCompatActivity() {

    private var activityCallbacks: MutableMap<Int, ActivityResult.ActivityCallback?> = HashMap()
    private var permissionsCallbacks: MutableMap<Int, ActivityResult.PermissionsCallback?> =
        HashMap()

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val callback: ActivityResult.ActivityCallback? = activityCallbacks[requestCode]
        callback?.onActivityResult(resultCode, data)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        val callback: ActivityResult.PermissionsCallback? = permissionsCallbacks[requestCode]
        val length: Int = grantResults.size
        for (i in 0 until length) {
            if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, permissions[i])) {
                    callback?.denyAndNotAskAgain()
                } else {
                    callback?.deny()
                }
                return
            }
        }
        if (length > 0) {
            callback?.allow()
        }
    }

    fun startForResult(intent: Intent, callback: ActivityResult.ActivityCallback?) {
        val requestCode: Int = Random().nextInt(0x0000FFFF)
        activityCallbacks[requestCode] = callback
        startActivityForResult(intent, requestCode)
        FullDialog().show(supportFragmentManager, null)
    }

    fun requestForPermissions(
        permissions: Array<String>,
        callback: ActivityResult.PermissionsCallback?
    ) {
        for (permission in permissions) {
            if (ActivityCompat.checkSelfPermission(this, permission)
                == PackageManager.PERMISSION_DENIED
            ) {
                val requestCode: Int = Random().nextInt(0x0000FFFF)
                permissionsCallbacks[requestCode] = callback
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    requestPermissions(permissions, requestCode)
                }
                return
            }
        }
        callback?.allow()
    }
}