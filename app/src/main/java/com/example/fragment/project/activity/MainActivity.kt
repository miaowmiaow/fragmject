package com.example.fragment.project.activity

import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.WindowManager
import com.example.fragment.library.base.bus.SimpleLiveBus
import com.example.fragment.library.base.utils.MetricsUtils
import com.example.fragment.library.common.activity.RouterActivity
import com.example.fragment.library.common.bean.UserBean
import com.example.fragment.library.common.constant.LiveBus
import com.example.fragment.library.common.constant.NavMode
import com.example.fragment.library.common.constant.Router
import com.example.fragment.library.common.fragment.WebFragment
import com.example.fragment.library.common.utils.WanHelper
import com.example.fragment.module.home.fragment.SearchFragment
import com.example.fragment.module.system.fragment.SystemListFragment
import com.example.fragment.project.R
import com.example.fragment.project.databinding.ActivityMainBinding
import com.example.fragment.project.fragment.MainFragment
import com.example.fragment.user.fragment.*
import java.io.File

class MainActivity : RouterActivity() {

    private var userId: String? = null
    private var mediaRecorder: MediaRecorder? = null
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null

    override fun frameLayoutId(): Int {
        return R.id.frame_layout
    }

    /**
     * 导航方法，根据路由名跳转切换Fragment
     */
    override fun navigation(name: Router, bundle: Bundle?, onBack: Boolean, navMode: NavMode) {
        when (name) {
            Router.MAIN ->
                switcher(MainFragment::class.java, bundle, false, navMode)
            Router.LOGIN ->
                switcher(LoginFragment::class.java, bundle, onBack, navMode)
            Router.REGISTER ->
                switcher(RegisterFragment::class.java, bundle, onBack, navMode)
            Router.WEB ->
                switcher(WebFragment::class.java, bundle, onBack, navMode)
            Router.SEARCH ->
                switcher(SearchFragment::class.java, bundle, onBack, navMode)
            Router.SYSTEM_LIST ->
                switcher(SystemListFragment::class.java, bundle, onBack, navMode)
            Router.COIN_RANK ->
                switcher(CoinRankFragment::class.java, bundle, onBack, navMode)
            Router.USER_SHARE ->
                switcher(UserShareFragment::class.java, bundle, onBack, navMode)
            Router.SETTING ->
                switcher(SettingFragment::class.java, bundle, onBack, navMode)
            else -> {
                if (isLogin()) {
                    when (name) {
                        Router.MY_COIN ->
                            switcher(MyCoinFragment::class.java, bundle, onBack, navMode)
                        Router.MY_COLLECT_ARTICLE ->
                            switcher(MyCollectArticleFragment::class.java, bundle, onBack, navMode)
                        Router.MY_SHARE_ARTICLE ->
                            switcher(MyShareArticleFragment::class.java, bundle, onBack, navMode)
                        Router.SHARE_ARTICLE ->
                            switcher(ShareArticleFragment::class.java, bundle, onBack, navMode)
                        else -> switcher(MainFragment::class.java, bundle, onBack, navMode)
                    }
                } else {
                    switcher(LoginFragment::class.java, bundle, onBack, navMode)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.AppTheme)
        window.setFormat(PixelFormat.TRANSLUCENT)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN)
        setContentView(ActivityMainBinding.inflate(LayoutInflater.from(this)).root)
        initUIMode()
        setupView()
        update()
    }

    override fun onStart() {
        super.onStart()
        WanHelper.getUser().observe(this, { userBean ->
            SimpleLiveBus.with<UserBean>(LiveBus.USER_STATUS_UPDATE).postEvent(userBean)
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        stopRecord()
    }

    private fun setupView() {
        navigation(Router.MAIN)
    }

    private fun update() {
        SimpleLiveBus.with<UserBean>(LiveBus.USER_STATUS_UPDATE).observe(this, { userBean ->
            userId = userBean.id
        })
    }

    /**
     * 登录状态校验
     */
    private fun isLogin(): Boolean {
        return userId != null && userId.toString().isNotBlank()
    }

    /**
     * 开始录屏，需要申请录屏权限
     */
    override fun startRecord(resultCode: Int, resultData: Intent) {
        try {
            mediaRecorder = MediaRecorder()
            //MediaProjectionManager申请权限  MediaProjection获取申请结果,防止别人调取隐私
            val projectionManager =
                getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            mediaProjection =
                projectionManager.getMediaProjection(resultCode, resultData)
            //设置音频源
            mediaRecorder?.setAudioSource(MediaRecorder.AudioSource.MIC)
            //设置视频源
            mediaRecorder?.setVideoSource(MediaRecorder.VideoSource.SURFACE)
            //设置输出的编码格式
            mediaRecorder?.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            val moviesPath = getExternalFilesDir(Environment.DIRECTORY_MOVIES)?.absolutePath
                ?: cacheDir.absolutePath
            val recordPath =
                moviesPath + File.separator + System.currentTimeMillis() + ".mp4"
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
            //设置帧率，该帧率必须是硬件支持的，
            //可以通过Camera.CameraParameter.getSupportedPreviewFpsRange()方法获取相机支持的帧率
            mediaRecorder?.setVideoFrameRate(24)
            mediaRecorder?.prepare()
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
            mediaRecorder?.start()
        } catch (e: Exception) {
            e.printStackTrace()
            showTips("录屏失败:${e.message}")
        }
    }

    /**
     * 停止录屏
     */
    override fun stopRecord() {
        try {
            mediaRecorder?.stop()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            mediaRecorder?.reset()
            mediaRecorder?.release()
            virtualDisplay?.release()
            mediaProjection?.stop()
            mediaRecorder = null
            virtualDisplay = null
            mediaProjection = null
        }
    }

}