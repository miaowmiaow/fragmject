package com.example.miaow.base.utils

import android.Manifest
import android.app.Activity
import android.content.Context.MEDIA_PROJECTION_SERVICE
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.example.miaow.base.service.MediaService.Companion.startMediaService
import com.example.miaow.base.service.MediaService.Companion.stopMediaService
import java.io.File

interface ScreenCaptureCallback {
    fun onActivityResult(resultCode: Int, message: String)
}

fun FragmentActivity.startScreenCapture(callback: ScreenCaptureCallback?) {
    val tag = ScreenCaptureFragment::class.java.simpleName
    var fragment = supportFragmentManager.findFragmentByTag(tag)
    if (fragment == null) {
        fragment = ScreenCaptureFragment.newInstance()
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        fragmentTransaction.add(fragment, tag)
        fragmentTransaction.commitAllowingStateLoss()
        supportFragmentManager.executePendingTransactions()
    }
    if (fragment is ScreenCaptureFragment) {
        fragment.startScreenCapture(callback)
    }
}

fun FragmentActivity.stopScreenCapture() {
    val tag = ScreenCaptureFragment::class.java.simpleName
    val fragment = supportFragmentManager.findFragmentByTag(tag)
    val fragmentTransaction = supportFragmentManager.beginTransaction()
    if (fragment != null && fragment is ScreenCaptureFragment) {
        fragment.stopScreenCapture()
        fragmentTransaction.remove(fragment)
    }
    fragmentTransaction.commitAllowingStateLoss()
    supportFragmentManager.executePendingTransactions()
}

class ScreenCaptureFragment : Fragment() {

    private lateinit var savePath: String
    private var mediaRecorder: MediaRecorder? = null
    private var mediaProjectionManager: MediaProjectionManager? = null
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var callback: ScreenCaptureCallback? = null

    private val requestMediaProjectionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val code = result.resultCode
            val data = result.data
            if (code != Activity.RESULT_OK || data == null) {
                callback?.onActivityResult(result.resultCode, "屏幕录制异常:data = null")
                stopScreenCapture()
            } else {
                try {
                    val width = convertOddToEven(resources.displayMetrics.widthPixels)
                    val height = convertOddToEven(resources.displayMetrics.heightPixels)
                    mediaRecorder?.setAudioSource(MediaRecorder.AudioSource.MIC)//设置音频源
                    mediaRecorder?.setVideoSource(MediaRecorder.VideoSource.SURFACE)//设置视频源
                    mediaRecorder?.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)//设置输出的编码格式
                    mediaRecorder?.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)//音频编码
                    mediaRecorder?.setVideoEncoder(MediaRecorder.VideoEncoder.H264)//图像编码
                    //设置录屏时屏幕大小,这个可跟VirtualDisplay一起控制屏幕大小
                    // VirtualDisplay是将屏幕设置成多大多小setVideoSize是输出文件时屏幕多大多小,需要传偶数否则会报错
                    mediaRecorder?.setVideoSize(width, height)
                    mediaRecorder?.setVideoEncodingBitRate((width * height * 2.6).toInt())//设置码率
                    mediaRecorder?.setVideoFrameRate(24)//设置帧率，该帧率必须是硬件支持的，可以通过Camera.CameraParameter.getSupportedPreviewFpsRange()方法获取相机支持的帧率
                    mediaRecorder?.setOutputFile(savePath)
                    mediaRecorder?.prepare()
                    mediaRecorder?.start()
                    mediaProjection = mediaProjectionManager?.getMediaProjection(code, data)
                    virtualDisplay = mediaProjection?.createVirtualDisplay(
                        "Screen Capture Service",
                        resources.displayMetrics.widthPixels,
                        resources.displayMetrics.heightPixels,
                        resources.displayMetrics.density.toInt(),
                        DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                        mediaRecorder?.surface,
                        null,
                        null
                    )
                    callback?.onActivityResult(Activity.RESULT_OK, "屏幕录制中")
                } catch (e: Exception) {
                    callback?.onActivityResult(
                        Activity.RESULT_CANCELED,
                        "屏幕录制异常:${e.message}"
                    )
                    stopScreenCapture()
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val moviesPath = CacheUtils.getDirPath(requireActivity(), "movies")
        savePath = moviesPath + File.separator + System.currentTimeMillis() + ".mp4"
        mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            MediaRecorder(requireActivity())
        } else {
            MediaRecorder()
        }
        mediaProjectionManager =
            requireActivity().getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    }

    override fun onDestroy() {
        super.onDestroy()
        callback = null
    }

    /**
     * 将一个奇数转换为小于它1的偶数
     * 计算机中 -2 的补码表示为 11111110
     * 对一个奇数 a 进行 a & 11111110 运算，会将 a 的最后一位二进制位置为 0，从而将奇数转换为偶数
     * 例如：奇数 a=7，二进制表示为 00000111，经过 a & 11111110 运算后得到 00000110，即偶数 6
     */
    private fun convertOddToEven(a: Int): Int {
        return a and -2
    }

    fun startScreenCapture(callback: ScreenCaptureCallback?) {
        this.callback = callback
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
                requestMediaProjectionLauncher.launch(mediaProjectionManager?.createScreenCaptureIntent())
            }

            override fun deny() {
                callback?.onActivityResult(Activity.RESULT_CANCELED, "没有麦克风权限")
                stopScreenCapture()
            }
        })

    }

    fun stopScreenCapture() {
        callback?.onActivityResult(Activity.RESULT_CANCELED, "屏幕录制结束")
        try {
            mediaRecorder?.stop()
            mediaRecorder?.reset()
            mediaRecorder?.release()
            virtualDisplay?.release()
            mediaProjection?.stop()
            requireActivity().saveVideoToAlbum(File(savePath)) { _, _ -> }
        } catch (e: Exception) {
            Log.e(this.javaClass.name, e.message.toString())
        } finally {
            mediaRecorder = null
            virtualDisplay = null
            mediaProjection = null
            requireActivity().stopMediaService()
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(): ScreenCaptureFragment {
            return ScreenCaptureFragment()
        }
    }

}