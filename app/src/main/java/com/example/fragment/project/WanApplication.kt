package com.example.fragment.project

import android.app.Application
import android.content.ComponentCallbacks2
import android.os.Build
import coil.ComponentRegistry
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.decode.SvgDecoder
import coil.decode.VideoFrameDecoder
import com.example.fragment.project.ui.web.WebViewManager
import com.example.miaow.base.http.OkHelper
import com.example.miaow.base.http.setBaseUrl
import com.example.miaow.base.http.setHttpClientLazy

class WanApplication : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()
        setBaseUrl("https://www.wanandroid.com/")
        // 只注册 provider，不在主线程同步构造 OkHttpClient；
        // 首个网络请求发起时才会在背景创建，冷启动主线程耗时明显下降。
        setHttpClientLazy { OkHelper.httpClient(applicationContext) }
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(applicationContext)
            .crossfade(true)
            .okHttpClient { OkHelper.httpClient(applicationContext) }
            .components(fun ComponentRegistry.Builder.() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
                add(SvgDecoder.Factory())
                add(VideoFrameDecoder.Factory())
            })
            .build()
    }

    /**
     * 响应系统内存压力：把 WebView 池按档位分级释放。
     *
     * - UI 隐藏（应用切到后台）：先把 keep-alive 池清空，仅保留 1 个空闲热身实例，
     *   既显著降低后台内存占用，又能让用户回到前台时秒开。
     * - 应用进程已被放进 LRU 后台名单且系统资源紧张：彻底释放，避免被系统直接 kill。
     * - 前台运行但系统内存紧张：同上，按等级降级保活策略。
     *
     * WebView 单实例约 30~80MB，4 个 keep-alive 在低端机上明显是 OOM 隐患来源；
     * 主动响应 onTrimMemory 比被动等待 GC/被 kill 友好得多。
     */
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        when (level) {
            // UI 完全隐藏：用户切到后台/锁屏；保留 spare，清空 keep-alive。
            ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN -> WebViewManager.trimToSpare()
            // 进程仍在前台，但系统内存紧张：先做温和回收。
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE,
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW -> WebViewManager.trimToSpare()
            // 进程已进入后台 LRU，且系统内存严重不足：彻底释放，争取不被 kill。
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL,
            ComponentCallbacks2.TRIM_MEMORY_BACKGROUND,
            ComponentCallbacks2.TRIM_MEMORY_MODERATE,
            ComponentCallbacks2.TRIM_MEMORY_COMPLETE -> WebViewManager.releaseAll()
            else -> Unit
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
        // 老接口的极端兜底：直接释放全部 WebView。
        WebViewManager.releaseAll()
    }

}