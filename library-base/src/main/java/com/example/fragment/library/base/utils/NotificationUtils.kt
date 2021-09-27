package com.example.fragment.library.base.utils

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat

object NotificationUtils {

    fun areNotificationsEnabled(context: Context): Boolean {
        val manager = NotificationManagerCompat.from(context)
        return manager.areNotificationsEnabled().also { enable ->
            println(
                if (enable) {
                    """
                    通知权限已经被打开
                    手机型号:${Build.MODEL}
                    SDK版本:${Build.VERSION.SDK_INT}
                    系统版本:${Build.VERSION.RELEASE}
                    软件包名:${context.packageName}
                    """.trimIndent()
                } else {
                    "通知权限未开启"
                }
            )
        }
    }

    fun gotoNotificationSettings(context: Context) {
        try {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                    val intent = Intent()
                    intent.action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                    intent.putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    intent.putExtra(Settings.EXTRA_CHANNEL_ID, context.applicationInfo.uid)
                    context.startActivity(intent)
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP -> {
                    val intent = Intent()
                    intent.action = "android.settings.APP_NOTIFICATION_SETTINGS"
                    intent.putExtra("app_package", context.packageName)
                    intent.putExtra("app_uid", context.applicationInfo.uid)
                    context.startActivity(intent)
                }
                else -> {
                    SystemUtil.gotoAppDetailsSettings(context)
                }
            }
        } catch (e: Exception) {
            SystemUtil.gotoAppDetailsSettings(context)
        }
    }
}