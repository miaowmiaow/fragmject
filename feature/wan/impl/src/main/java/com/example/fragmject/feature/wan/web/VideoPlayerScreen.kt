package com.example.fragmject.feature.wan.web

import android.content.pm.ActivityInfo
import android.view.View
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.media3.common.MediaItem
import com.example.fragmject.core.ui.components.ExoPlayer

/**
 * 播放本地视频文件的全屏播放器。
 *
 * - 进入时强制横屏 + 隐藏系统栏（沉浸模式）
 * - 退出时恢复竖屏 + 恢复系统栏
 */
@Suppress("DEPRECATION")
@Composable
fun VideoPlayerScreen(filePath: String, onNavigateUp: () -> Unit = {}) {
    val activity = LocalActivity.current ?: return
    val window = activity.window
    val decorView = window.decorView

    DisposableEffect(window) {
        // 1) 锁定横屏
        val originalOrientation = activity.requestedOrientation
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE

        // 2) 沉浸全屏：使用 systemUiVisibility 标志位，比 WindowInsetsControllerCompat
        //    对 PlayerView 更可靠（PlayerView 不会覆盖这些标志位）
        val originalUiVisibility = decorView.systemUiVisibility
        decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        )

        // 3) 当系统栏因滑动短暂显示后自动恢复隐藏
        val visibilityListener = View.OnSystemUiVisibilityChangeListener { visibility ->
            if (visibility and View.SYSTEM_UI_FLAG_FULLSCREEN == 0) {
                decorView.postDelayed({
                    decorView.systemUiVisibility = (
                        View.SYSTEM_UI_FLAG_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    )
                }, 100)
            }
        }
        decorView.setOnSystemUiVisibilityChangeListener(visibilityListener)

        onDispose {
            // 恢复竖屏
            activity.requestedOrientation = if (originalOrientation == ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) {
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            } else {
                originalOrientation
            }
            // 恢复系统栏
            decorView.systemUiVisibility = originalUiVisibility
            decorView.setOnSystemUiVisibilityChangeListener(null)
        }
    }

    ExoPlayer(
        mediaItems = listOf(MediaItem.fromUri("file://$filePath")),
        modifier = Modifier.fillMaxSize()
    )
}
