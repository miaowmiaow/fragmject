package com.example.miaow.base.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.fragment.app.FragmentActivity
import com.example.miaow.base.R

class MediaService : Service() {

    companion object {
        const val NOTICE_ID = 20220815

        fun FragmentActivity.startMediaService() {
            startService(Intent(this, MediaService::class.java))
        }

        fun FragmentActivity.stopMediaService() {
            stopService(Intent(this, MediaService::class.java))
        }
    }

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val id = packageName
            val name = javaClass.name
            val channel = NotificationChannel(id, name, NotificationManager.IMPORTANCE_HIGH)
            val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
            startForeground(
                NOTICE_ID, NotificationCompat.Builder(applicationContext, id)
                    .setContentTitle("屏幕分享服务~")
                    .setContentText("正在录制/投射您屏幕上显示的所有内容，请注意保护个人敏感信息。")
                    .setSmallIcon(R.drawable.logo)
                    .build()
            )
        } else {
            startForeground(
                NOTICE_ID, NotificationCompat.Builder(applicationContext)
                    .setContentTitle("屏幕分享服务~")
                    .setContentText("正在录制/投射您屏幕上显示的所有内容，请注意保护个人敏感信息。")
                    .setSmallIcon(R.drawable.logo)
                    .build()
            )
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

}