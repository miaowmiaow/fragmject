package com.example.fragment.library.base.utils

import android.Manifest.permission.*
import android.app.Activity
import android.content.Context.MEDIA_PROJECTION_SERVICE
import android.content.Intent
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import androidx.fragment.app.FragmentActivity
import com.example.fragment.library.base.dialog.PermissionDialog
import com.example.fragment.library.base.utils.ActivityResultHelper.requestPermissions
import com.example.fragment.library.base.utils.ActivityResultHelper.startForResult

object ScreenRecordHelper {

    private var mediaProjection: MediaProjection? = null
    private val mp get() = mediaProjection!!
    private var mediaRecorder: MediaRecorder? = null
    private val mr get() = mediaRecorder!!
    private var virtualDisplay: VirtualDisplay? = null
    private val vd get() = virtualDisplay!!
    private var isRunning = false

    /**
     * 开始录屏，需要申请录屏权限
     */
    fun FragmentActivity.startScreenRecord(onCallback: (Int, String) -> Unit) {
        if (isRunning) {
            onCallback.invoke(Activity.RESULT_OK, "屏幕录制中")
            return
        }
        requestScreenRecord { resultCode, resultData, resultMessage ->
            if (resultCode == Activity.RESULT_OK && resultData != null) {
                try {
                    if (mediaProjection == null) {
                        //MediaProjectionManager申请权限MediaProjection获取申请结果,防止别人调取隐私
                        val service = getSystemService(MEDIA_PROJECTION_SERVICE)
                        val manager = service as MediaProjectionManager
                        mediaProjection = manager.getMediaProjection(resultCode, resultData)
                    }
                    if (mediaRecorder == null) {
                        mediaRecorder = MediaRecorder().apply {
                            setAudioSource(MediaRecorder.AudioSource.MIC)//设置音频源
                            setVideoSource(MediaRecorder.VideoSource.SURFACE)//设置视频源
                            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)//设置输出的编码格式
                            val path = CacheUtils.getCacheDirectory(
                                this@startScreenRecord,
                                "movies"
                            ).absolutePath
                            val recordPath = path + "/" + System.currentTimeMillis() + ".mp4"
                            setOutputFile(recordPath)
                            //设置录屏时屏幕大小,这个可跟VirtualDisplay一起控制屏幕大小,VirtualDisplay是将屏幕设置成多大多小setVideoSize是输出文件时屏幕多大多小
                            setVideoSize(MetricsUtils.screenWidth, MetricsUtils.screenHeight)
                            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)//音频编码
                            setVideoEncoder(MediaRecorder.VideoEncoder.H264)//图像编码
                            val bitRate = MetricsUtils.screenWidth * MetricsUtils.screenHeight * 2.6
                            setVideoEncodingBitRate(bitRate.toInt())//设置码率
                            setVideoFrameRate(24)//设置帧率，该帧率必须是硬件支持的，可以通过Camera.CameraParameter.getSupportedPreviewFpsRange()方法获取相机支持的帧率
                            prepare()
                        }
                    }
                    if (virtualDisplay == null) {
                        //获取录制屏幕的大小,像素,等等一些数据
                        virtualDisplay = mp.createVirtualDisplay(
                            "Screen Record Service",
                            MetricsUtils.screenWidth,
                            MetricsUtils.screenHeight,
                            MetricsUtils.densityDpi,
                            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                            mr.surface,
                            null, null
                        )
                    }
                    mr.start()
                    isRunning = true
                } catch (e: Exception) {
                    onCallback.invoke(Activity.RESULT_CANCELED, "屏幕录制异常:${e.message}")
                }
            }
            onCallback.invoke(resultCode, resultMessage)
        }
    }

    /**
     * 停止录屏
     */
    fun FragmentActivity.stopScreenRecord() {
        if (!isRunning) {
            return
        }
        try {
            mr.stop()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            mr.reset()
            mr.release()
            vd.release()
            mp.stop()
            mediaRecorder = null
            virtualDisplay = null
            mediaProjection = null
        }
    }

    /**
     * 申请录屏权限
     */
    private fun FragmentActivity.requestScreenRecord(onCallback: (Int, Intent?, String) -> Unit) {
        val permissions = arrayOf(READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE, RECORD_AUDIO)
        requestPermissions(permissions, object : PermissionsCallback {

            override fun allow() {
                val manager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                startForResult(manager.createScreenCaptureIntent(), object : ActivityCallback {

                    override fun onActivityResult(resultCode: Int, data: Intent?) {
                        if (resultCode == Activity.RESULT_OK) {
                            onCallback.invoke(resultCode, data, "屏幕录制中")
                        } else {
                            onCallback.invoke(resultCode, null, "没有屏幕录制权限")
                        }
                    }
                })
            }

            override fun deny() {
                PermissionDialog.alert(this@requestScreenRecord, "存储和麦克风")
                onCallback.invoke(Activity.RESULT_CANCELED, null, "没有存储和麦克风权限")
            }
        })
    }

}