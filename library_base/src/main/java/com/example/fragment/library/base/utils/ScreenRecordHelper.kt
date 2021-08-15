package com.example.fragment.library.base.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Environment
import androidx.fragment.app.FragmentActivity
import com.example.fragment.library.base.dialog.PermissionDialog
import com.example.fragment.library.base.utils.ActivityResultHelper.requestRecordAudioPermissions
import com.example.fragment.library.base.utils.ActivityResultHelper.requestStoragePermissions
import com.example.fragment.library.base.utils.ActivityResultHelper.startForResult
import java.io.File

object ScreenRecordHelper {

    private var mediaProjection: MediaProjection? = null
    private var mediaRecorder: MediaRecorder? = null
    private var virtualDisplay: VirtualDisplay? = null

    private var screenRecordData: Intent? = null
    private var isRunning = false

    /**
     * 开始录屏，需要申请录屏权限
     */
    fun FragmentActivity.startScreenRecord(onCallback: (Int, String) -> Unit) {
        if (isRunning) {
            onCallback.invoke(Activity.RESULT_OK, "屏幕录制中")
            return
        }
        requestScreenRecordPermissions { resultCode, resultData, resultMessage ->
            if (resultCode == Activity.RESULT_OK && resultData != null) {
                try {
                    if (mediaProjection == null) {
                        //MediaProjectionManager申请权限MediaProjection获取申请结果,防止别人调取隐私
                        val manager =
                            getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                        mediaProjection = manager.getMediaProjection(resultCode, resultData)
                    }
                    if (mediaRecorder == null) {
                        mediaRecorder = MediaRecorder()
                        //设置音频源
                        mediaRecorder?.setAudioSource(MediaRecorder.AudioSource.MIC)
                        //设置视频源
                        mediaRecorder?.setVideoSource(MediaRecorder.VideoSource.SURFACE)
                        //设置输出的编码格式
                        mediaRecorder?.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                        val moviesPath =
                            getExternalFilesDir(Environment.DIRECTORY_MOVIES)?.absolutePath
                                ?: cacheDir.absolutePath
                        val recordPath =
                            moviesPath + File.separator + System.currentTimeMillis() + ".mp4"
                        mediaRecorder?.setOutputFile(recordPath)
                        //设置录屏时屏幕大小,这个可跟mVirtualDisplay 一起控制屏幕大小
                        //virtualDisplay 是将屏幕设置成多大多小，setVideoSize是输出文件时屏幕多大多小
                        mediaRecorder?.setVideoSize(
                            MetricsUtils.screenWidth,
                            MetricsUtils.screenHeight
                        )
                        //音频编码
                        mediaRecorder?.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                        //图像编码
                        mediaRecorder?.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
                        val bitRate = MetricsUtils.screenWidth * MetricsUtils.screenHeight * 2.6
                        //设置码率
                        mediaRecorder?.setVideoEncodingBitRate(bitRate.toInt())
                        //设置帧率，该帧率必须是硬件支持的，
                        //可以通过Camera.CameraParameter.getSupportedPreviewFpsRange()方法获取相机支持的帧率
                        mediaRecorder?.setVideoFrameRate(24)
                        mediaRecorder?.prepare()
                    }
                    if (virtualDisplay == null) {
                        //获取录制屏幕的大小,像素,等等一些数据
                        virtualDisplay = mediaProjection?.createVirtualDisplay(
                            "Screen Record Service",
                            MetricsUtils.screenWidth,
                            MetricsUtils.screenHeight,
                            MetricsUtils.densityDpi,
                            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                            mediaRecorder?.surface,
                            null,
                            null
                        )
                    }
                    mediaRecorder?.start()
                    isRunning = true
                } catch (e: Exception) {
                    e.printStackTrace()
                    onCallback.invoke(Activity.RESULT_CANCELED, "录屏失败:${e.message}")
                }
            }
            onCallback.invoke(resultCode, resultMessage)
        }
    }

    /**
     * 继续录屏
     */
    fun FragmentActivity.resumeScreenRecord() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            mediaRecorder?.resume()
        }
    }

    /**
     * 暂停录屏
     */
    fun FragmentActivity.pauseScreenRecord() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            mediaRecorder?.pause()
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
            mediaRecorder?.stop()
            mediaRecorder?.reset()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            mediaRecorder?.release()
            virtualDisplay?.release()
            mediaProjection?.stop()
            mediaRecorder = null
            virtualDisplay = null
            mediaProjection = null
            isRunning = false
        }
    }

    /**
     * 申请录屏权限
     */
    private fun FragmentActivity.requestScreenRecordPermissions(onCallback: (Int, Intent?, String) -> Unit) {
        requestStoragePermissions(object : PermissionsCallback {

            override fun allow() {
                requestRecordAudioPermissions(object : PermissionsCallback {

                    override fun allow() {
                        if (screenRecordData != null) {
                            onCallback.invoke(Activity.RESULT_OK, screenRecordData, "屏幕录制中")
                        } else {
                            val service = getSystemService(Context.MEDIA_PROJECTION_SERVICE)
                            val manager = service as MediaProjectionManager
                            val intent = manager.createScreenCaptureIntent()
                            val activityCallback = object : ActivityCallback {
                                override fun onActivityResult(resultCode: Int, data: Intent?) {
                                    if (resultCode == Activity.RESULT_OK) {
                                        onCallback.invoke(resultCode, data, "屏幕录制中")
                                        screenRecordData = data
                                    } else {
                                        onCallback.invoke(resultCode, null, "没有屏幕录制权限")
                                    }
                                }
                            }
                            startForResult(intent, activityCallback)
                        }
                    }

                    override fun deny() {
                        PermissionDialog.alert(this@requestScreenRecordPermissions, "麦克风")
                        onCallback.invoke(Activity.RESULT_CANCELED, null, "没有麦克风权限")
                    }
                })
            }

            override fun deny() {
                PermissionDialog.alert(this@requestScreenRecordPermissions, "存储")
                onCallback.invoke(Activity.RESULT_CANCELED, null, "没有存储权限")
            }
        })
    }

}