package com.example.miaow.base.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.view.View

fun Context.getMetaData(key: String): String {
    var metaData = ""
    try {
        packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA).let {
            it.metaData.getString(key)?.let { value ->
                metaData = value
            }
        }
    } catch (e: Exception) {
        Log.e(this.javaClass.name, e.message.toString())
    }
    return metaData
}

/**
 * 获取应用程序名称
 */
fun Context.getAppName(): String {
    var name = ""
    try {
        packageManager.getPackageInfo(packageName, 0).let {
            name = resources.getString(it.applicationInfo!!.labelRes)
        }
    } catch (e: Exception) {
        Log.e(this.javaClass.name, e.message.toString())
    }
    return name
}

/**
 * 获取应用程序版本名称
 */
fun Context.getVersionName(): String {
    var name = ""
    try {
        packageManager.getPackageInfo(packageName, 0).let {
            name = it.versionName.toString()
        }
    } catch (e: Exception) {
        Log.e(this.javaClass.name, e.message.toString())
    }
    return name
}

/**
 * 获取应用程序版本号
 */
fun Context.getVersionCode(): Long {
    var code = 0L
    try {
        packageManager.getPackageInfo(packageName, 0).let {
            code = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                it.longVersionCode
            } else {
                it.versionCode.toLong()
            }
        }
    } catch (e: Exception) {
        Log.e(this.javaClass.name, e.message.toString())
    }
    return code
}

/**
 * 跳转应用详情
 */
fun Context.gotoAppDetailsSettings() {
    val intent = Intent()
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
    intent.data = Uri.fromParts("package", packageName, null)
    startActivity(intent)
}

/**
 * app灰白化,特殊节日使用
 */
fun Activity.appGraying() {
    val paint = Paint()
    val colorMatrix = ColorMatrix()
    colorMatrix.setSaturation(0f)
    paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
    window.decorView.setLayerType(View.LAYER_TYPE_HARDWARE, paint)
}