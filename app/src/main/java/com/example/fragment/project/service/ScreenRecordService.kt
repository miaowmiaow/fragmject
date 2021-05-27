package com.example.fragment.project.service

import android.app.Service
import android.content.*
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.*
import androidx.annotation.RequiresApi
import com.example.fragment.library.base.utils.MetricsUtils
import java.io.File

class ScreenRecordService : Service() {

    companion object {

        fun bindService(context: Context, connection: ServiceConnection) {
            context.bindService(
                Intent(context, ScreenRecordService::class.java),
                connection,
                Context.BIND_AUTO_CREATE
            )
        }

        fun unbindService(context: Context, connection: ServiceConnection) {
            context.unbindService(connection)
        }
    }

    private var mediaRecorder: MediaRecorder? = null
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null

    private var isRunning = false

    override fun onBind(intent: Intent?): IBinder {
        return ScreenRecordBinder()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        clearRecord()
    }

    fun startRecord(resultCode: Int, resultData: Intent): Boolean {
        if (isRunning) {
            return false
        }
        isRunning = true
        try {
            mediaRecorder = MediaRecorder()
            //MediaProjectionManager申请权限  MediaProjection获取申请结果,防止别人调取隐私
            val projectionManager =
                getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            mediaProjection = projectionManager.getMediaProjection(resultCode, resultData)
            //设置音频源
            mediaRecorder?.setAudioSource(MediaRecorder.AudioSource.MIC)
            //设置视频源
            mediaRecorder?.setVideoSource(MediaRecorder.VideoSource.SURFACE)
            //设置输出的编码格式
            mediaRecorder?.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            val moviesPath = getExternalFilesDir(Environment.DIRECTORY_MOVIES)?.absolutePath
                ?: cacheDir.absolutePath
            val recordPath = moviesPath + File.separator + System.currentTimeMillis() + ".mp4"
            mediaRecorder?.setOutputFile(recordPath)
            //设置录屏时屏幕大小,这个可跟mVirtualDisplay 一起控制屏幕大小
            //virtualDisplay 是将屏幕设置成多大多小，setVideoSize是输出文件时屏幕多大多小
            mediaRecorder?.setVideoSize(MetricsUtils.screenWidth, MetricsUtils.screenHeight)
            //音频编码
            mediaRecorder?.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            //图像编码
            mediaRecorder?.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            val bitRate = MetricsUtils.screenWidth * MetricsUtils.screenHeight * 2.6
            //设置码率
            mediaRecorder?.setVideoEncodingBitRate(bitRate.toInt())
            //设置帧率，该帧率必须是硬件支持的，可以通过Camera.CameraParameter.getSupportedPreviewFpsRange()方法获取相机支持的帧率
            mediaRecorder?.setVideoFrameRate(24)
            mediaRecorder?.prepare()
            //获取录制屏幕的大小,像素,等等一些数据
            virtualDisplay = mediaProjection?.createVirtualDisplay(
                "Screen Record Service",
                MetricsUtils.screenWidth, MetricsUtils.screenHeight, MetricsUtils.densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mediaRecorder?.surface,
                null, null
            )
            mediaRecorder?.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return true
    }

    fun resumeRecord() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            mediaRecorder?.resume()
        }
    }

    fun pauseRecord() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            mediaRecorder?.pause()
        }
    }

    fun stopRecord(): Boolean {
        if (!isRunning) {
            return false
        }
        isRunning = false
        try {
            mediaRecorder?.stop()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            clearRecord()
        }
        return true
    }

    private fun clearRecord() {
        mediaRecorder?.reset()
        mediaRecorder?.release()
        virtualDisplay?.release()
        mediaProjection?.stop()
        mediaRecorder = null
        virtualDisplay = null
        mediaProjection = null
    }

    inner class ScreenRecordBinder : Binder() {

        fun startRecord(resultCode: Int, resultData: Intent): Boolean {
            return this@ScreenRecordService.startRecord(resultCode, resultData)
        }

        fun resumeRecord() {
            this@ScreenRecordService.resumeRecord()
        }

        fun pauseRecord() {
            this@ScreenRecordService.pauseRecord()
        }

        fun stopRecord(): Boolean {
            return this@ScreenRecordService.stopRecord()
        }

    }

}