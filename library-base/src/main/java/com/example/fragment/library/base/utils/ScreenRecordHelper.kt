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
import com.example.fragment.library.base.service.MediaService.Companion.startMediaService
import com.example.fragment.library.base.service.MediaService.Companion.stopMediaService

object ScreenRecordHelper {

    private var mediaProjection: MediaProjection? = null
    private var mediaRecorder: MediaRecorder? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var isRunning = false

    /**
     * 开始录屏，需要申请录屏权限
     */
    fun FragmentActivity.startScreenRecord(onCallback: (Int, String) -> Unit) {
        if (isRunning) {
            onCallback.invoke(Activity.RESULT_OK, "屏幕录制中")
            return
        }
        startMediaService()
        val permissions = arrayOf(READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE, RECORD_AUDIO)
        requestPermissions(permissions, object : PermissionsCallback {

            override fun allow() {
                val manager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                startForResult(manager.createScreenCaptureIntent(), object : ActivityCallback {

                    override fun onActivityResult(resultCode: Int, data: Intent?) {
                        if (resultCode != Activity.RESULT_OK || data == null) {
                            onCallback.invoke(resultCode, "没有屏幕录制权限")
                            return
                        }
                        try {
                            if (mediaProjection == null) {
                                //MediaProjectionManager申请权限MediaProjection获取申请结果,防止别人调取隐私
                                mediaProjection = manager.getMediaProjection(resultCode, data)
                            }
                            if (mediaRecorder == null) {
                                mediaRecorder = MediaRecorder()
                                mediaRecorder?.setAudioSource(MediaRecorder.AudioSource.MIC)//设置音频源
                                mediaRecorder?.setVideoSource(MediaRecorder.VideoSource.SURFACE)//设置视频源
                                mediaRecorder?.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)//设置输出的编码格式
                                //设置录屏时屏幕大小,这个可跟VirtualDisplay一起控制屏幕大小,VirtualDisplay是将屏幕设置成多大多小setVideoSize是输出文件时屏幕多大多小,需要传偶数否则会报错
                                mediaRecorder?.setVideoSize(screenWidth(), screenHeight())
                                mediaRecorder?.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)//音频编码
                                mediaRecorder?.setVideoEncoder(MediaRecorder.VideoEncoder.H264)//图像编码
                                val bitRate = screenWidth() * screenHeight() * 2.6
                                mediaRecorder?.setVideoEncodingBitRate(bitRate.toInt())//设置码率
                                mediaRecorder?.setVideoFrameRate(24)//设置帧率，该帧率必须是硬件支持的，可以通过Camera.CameraParameter.getSupportedPreviewFpsRange()方法获取相机支持的帧率
                                val path = CacheUtils.getDirPath(this@startScreenRecord, "movies")
                                val recordPath = path + "/" + System.currentTimeMillis() + ".mp4"
                                mediaRecorder?.setOutputFile(recordPath)
                                mediaRecorder?.prepare()
                            }
                            if (virtualDisplay == null) {
                                //获取录制屏幕的大小,像素,等等一些数据
                                virtualDisplay = mediaProjection?.createVirtualDisplay(
                                    "Screen Record Service",
                                    screenWidth(),
                                    screenHeight(),
                                    densityDpi(),
                                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                                    mediaRecorder?.surface,
                                    null, null
                                )
                            }
                            mediaRecorder?.start()
                            isRunning = true
                            onCallback.invoke(Activity.RESULT_OK, "屏幕录制中")
                        } catch (e: Exception) {
                            e.printStackTrace()
                            onCallback.invoke(Activity.RESULT_CANCELED, "屏幕录制异常:${e.message}")
                        }
                    }
                })
            }

            override fun deny() {
                PermissionDialog.alert(this@startScreenRecord, "存储和麦克风")
                onCallback.invoke(Activity.RESULT_CANCELED, "没有存储和麦克风权限")
            }
        })
    }

    /**
     * 停止录屏
     */
    fun FragmentActivity.stopScreenRecord() {
        if (!isRunning) {
            return
        }
        stopMediaService()
        try {
            mediaRecorder?.stop()
            mediaRecorder?.reset()
            mediaRecorder?.release()
            virtualDisplay?.release()
            mediaProjection?.stop()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            mediaRecorder = null
            virtualDisplay = null
            mediaProjection = null
            isRunning = false
        }
    }

}