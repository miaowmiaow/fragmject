package com.example.miaow.base.utils

import android.Manifest
import android.app.Activity
import android.content.Context.MEDIA_PROJECTION_SERVICE
import android.content.Intent
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.miaow.base.service.MediaService.Companion.startMediaService
import com.example.miaow.base.service.MediaService.Companion.stopMediaService
import java.io.File

fun AppCompatActivity.startScreenRecord(callback: ScreenRecordCallback?) {
    val tag = ScreenRecordFragment::class.java.simpleName
    var fragment = supportFragmentManager.findFragmentByTag(tag)
    if (fragment == null) {
        fragment = ScreenRecordFragment.newInstance()
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        fragmentTransaction.add(fragment, tag)
        fragmentTransaction.commitAllowingStateLoss()
        supportFragmentManager.executePendingTransactions()
    }
    if (fragment is ScreenRecordFragment) {
        fragment.startScreenRecord(callback)
    }
}

fun AppCompatActivity.stopScreenRecord(callback: ScreenRecordCallback?) {
    val tag = ScreenRecordFragment::class.java.simpleName
    val fragment = supportFragmentManager.findFragmentByTag(tag)
    val fragmentTransaction = supportFragmentManager.beginTransaction()
    if (fragment != null && fragment is ScreenRecordFragment) {
        fragment.stopScreenRecord(callback)
        fragmentTransaction.remove(fragment)
    }
    fragmentTransaction.commitAllowingStateLoss()
    supportFragmentManager.executePendingTransactions()
}

interface ScreenRecordCallback {
    fun onActivityResult(resultCode: Int, message: String)
}

class ScreenRecordFragment : Fragment() {

    private var callback: ScreenRecordCallback? = null

    private var mediaProjectionManager: MediaProjectionManager? = null
    private var mediaProjection: MediaProjection? = null
    private var mediaRecorder: MediaRecorder? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var isRunning = false

    private var recordPath: String = ""

    private val startForResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode != Activity.RESULT_OK || result.data == null) {
                callback?.onActivityResult(result.resultCode, "没有屏幕录制权限")
                stopScreenRecord(callback)
                return@registerForActivityResult
            }
            try {
                getMediaRecorder().setAudioSource(MediaRecorder.AudioSource.MIC)//设置音频源
                getMediaRecorder().setVideoSource(MediaRecorder.VideoSource.SURFACE)//设置视频源
                getMediaRecorder().setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)//设置输出的编码格式
                //设置录屏时屏幕大小,这个可跟VirtualDisplay一起控制屏幕大小
                // VirtualDisplay是将屏幕设置成多大多小setVideoSize是输出文件时屏幕多大多小,需要传偶数否则会报错
                getMediaRecorder().setVideoSize(screenWidth(), screenHeight())
                getMediaRecorder().setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)//音频编码
                getMediaRecorder().setVideoEncoder(MediaRecorder.VideoEncoder.H264)//图像编码
                val bitRate = screenWidth() * screenHeight() * 2.6
                getMediaRecorder().setVideoEncodingBitRate(bitRate.toInt())//设置码率
                getMediaRecorder().setVideoFrameRate(24)//设置帧率，该帧率必须是硬件支持的，可以通过Camera.CameraParameter.getSupportedPreviewFpsRange()方法获取相机支持的帧率
                getMediaRecorder().setOutputFile(recordPath)
                getMediaRecorder().prepare()
                getVirtualDisplay(result.resultCode, result.data!!)
                getMediaRecorder().start()
                isRunning = true
                callback?.onActivityResult(Activity.RESULT_OK, "屏幕录制中")
            } catch (e: Exception) {
                callback?.onActivityResult(Activity.RESULT_CANCELED, "屏幕录制异常:${e.message}")
                stopScreenRecord(callback)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mediaProjectionManager =
            requireActivity().getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val moviesPath = CacheUtils.getDirPath(requireContext(), "movies")
        recordPath = moviesPath + File.separator + System.currentTimeMillis() + ".mp4"
    }

    override fun onDestroy() {
        super.onDestroy()
        callback = null
    }

    private fun getMediaProjection(resultCode: Int, resultData: Intent): MediaProjection {
        //MediaProjectionManager申请权限MediaProjection获取申请结果,防止别人调取隐私
        return mediaProjection ?: mediaProjectionManager!!.getMediaProjection(
            resultCode,
            resultData
        ).also {
            mediaProjection = it
        }
    }

    private fun getMediaRecorder(): MediaRecorder {
        return mediaRecorder ?: if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            MediaRecorder(requireContext())
        } else {
            MediaRecorder()
        }.also {
            mediaRecorder = it
        }
    }

    private fun getVirtualDisplay(resultCode: Int, resultData: Intent): VirtualDisplay {
        //获取录制屏幕的大小,像素,等等一些数据
        return virtualDisplay ?: getMediaProjection(resultCode, resultData).createVirtualDisplay(
            "Screen Record Service",
            screenWidth(),
            screenHeight(),
            densityDpi(),
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            getMediaRecorder().surface,
            null, null
        ).also {
            virtualDisplay = it
        }
    }

    fun startScreenRecord(callback: ScreenRecordCallback?) {
        this.callback = callback
        if (isRunning) {
            callback?.onActivityResult(Activity.RESULT_OK, "屏幕录制中")
            return
        }
        requireActivity().startMediaService()
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            arrayOf(
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.RECORD_AUDIO
            )
        else
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.RECORD_AUDIO
            )
        childFragmentManager.requestPermissions(permissions, object : PermissionsCallback {
            override fun allow() {
                mediaProjectionManager?.let {
                    startForResult.launch(it.createScreenCaptureIntent())
                }
            }

            override fun deny() {
                callback?.onActivityResult(Activity.RESULT_CANCELED, "没有麦克风权限")
                stopScreenRecord(callback)
            }
        })
    }

    fun stopScreenRecord(callback: ScreenRecordCallback?) {
        this.callback = callback
        requireActivity().stopMediaService()
        callback?.onActivityResult(Activity.RESULT_CANCELED, "屏幕录制结束")
        if (!isRunning) {
            return
        }
        try {
            getMediaRecorder().stop()
            getMediaRecorder().reset()
            getMediaRecorder().release()
            virtualDisplay?.release()
            mediaProjection?.stop()
            requireActivity().saveVideoToAlbum(File(recordPath)) { _, _ -> }
        } catch (e: Exception) {
            Log.e(this.javaClass.name, e.message.toString())
        } finally {
            mediaRecorder = null
            virtualDisplay = null
            mediaProjection = null
            isRunning = false
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(): ScreenRecordFragment {
            return ScreenRecordFragment()
        }
    }

}