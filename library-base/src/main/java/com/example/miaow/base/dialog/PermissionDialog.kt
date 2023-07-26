package com.example.miaow.base.dialog

import android.app.Activity
import android.os.Process
import androidx.appcompat.app.AlertDialog
import com.example.miaow.base.utils.gotoAppDetailsSettings
import kotlin.system.exitProcess

object PermissionDialog {

    fun alert(activity: Activity, title: String) {
        val alertDialog = AlertDialog.Builder(activity)
        alertDialog.setTitle("帮助")
        alertDialog.setMessage("当前应用缺少${title}权限。\n请点击\"设置\"-\"权限\"打开所需权限。")
        alertDialog.setPositiveButton("去设置") { _, _ ->
            activity.gotoAppDetailsSettings()
        }
        alertDialog.show()
    }

    fun storage(activity: Activity) {
        val alertDialog = AlertDialog.Builder(activity)
        alertDialog.setTitle("没有相关权限")
        alertDialog.setMessage("当前应用缺少存储空间权限。\n请点击\"设置\"-\"权限\"打开所需权限。")
        alertDialog.setPositiveButton("去设置") { _, _ ->
            activity.gotoAppDetailsSettings()
            Process.killProcess(Process.myPid())
            exitProcess(1)
        }
        alertDialog.show()
    }

}