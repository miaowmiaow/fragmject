package com.example.fragment.library.base.component.activity

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.fragment.library.base.R
import com.example.fragment.library.base.component.dialog.FullDialog
import com.example.fragment.library.base.utils.ActivityResult
import java.util.*
import kotlin.collections.HashMap

abstract class BaseActivity : AppCompatActivity() {

    private val activityCallbacks: MutableMap<Int, ActivityResult.ActivityCallback?> = HashMap()
    private val permissionsCallbacks: MutableMap<Int, ActivityResult.PermissionsCallback?> =
        HashMap()
    private val listeners: MutableMap<String, OnBackPressedListener> = HashMap()

    private var exitTime = 0L

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

    override fun onBackPressed() {
        for (name in listeners.keys) {
            val listener = listeners[name]
            if (verifyFragment(name) && listener != null && listener.onBackPressed()) {
                return
            }
        }
        if (supportFragmentManager.backStackEntryCount > 1) {
            supportFragmentManager.popBackStackImmediate()
        } else {
            if (System.currentTimeMillis() - exitTime > 2000) {
                Toast.makeText(this, getString(R.string.one_more_press_2_back), Toast.LENGTH_SHORT)
                    .show()
            } else {
                moveTaskToBack(true)
            }
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

    /**
     * 验证目标Fragment是否为当前显示Fragment
     *
     * @param fragmentName Fragment
     * @return boolean
     */
    private fun verifyFragment(fragmentName: String): Boolean {
        return supportFragmentManager.findFragmentByTag(fragmentName) != null
    }

    /**
     * 注册返回键监听事件
     * 注册成功后拦截返回键事件并传递给监听者
     *
     * @param fragmentName Fragment
     * @param listener     OnBackPressedListener
     */
    fun registerOnBackPressedListener(
        fragmentName: String,
        listener: OnBackPressedListener
    ) {
        listeners[fragmentName] = listener
    }

    /**
     * 移除返回键监听事件
     *
     * @param fragmentName Fragment
     */
    fun removerOnBackPressedListener(fragmentName: String) {
        listeners.remove(fragmentName)
    }

}

/**
 * 返回键监听事件
 */
interface OnBackPressedListener {
    fun onBackPressed(): Boolean
}