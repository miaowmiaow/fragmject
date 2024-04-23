package com.example.miaow.base.utils

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat

fun Context.areNotificationsEnabled(): Boolean {
    return NotificationManagerCompat.from(this).areNotificationsEnabled().also { enable ->
        println(
            if (enable) {
                """
                    通知权限已经被打开
                    手机型号:${Build.MODEL}
                    SDK版本:${Build.VERSION.SDK_INT}
                    系统版本:${Build.VERSION.RELEASE}
                    软件包名:${packageName}
                """.trimIndent()
            } else {
                "通知权限未开启"
            }
        )
    }
}

fun Context.gotoNotificationSettings() {
    try {
        val intent = Intent()
        intent.action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
        intent.putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        intent.putExtra(Settings.EXTRA_CHANNEL_ID, applicationInfo.uid)
        startActivity(intent)
    } catch (e: Exception) {
        gotoAppDetailsSettings()
    }
}