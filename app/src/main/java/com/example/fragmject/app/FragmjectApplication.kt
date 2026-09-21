package com.example.fragmject.app

import android.app.Application
import android.os.Build
import coil.ComponentRegistry
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.decode.SvgDecoder
import coil.decode.VideoFrameDecoder
import com.example.fragmject.core.webview.WebViewPool
import com.example.fragmject.core.network.di.CoilOkHttpClient
import com.example.fragmject.core.common.utils.CacheUtils
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import okhttp3.OkHttpClient

@HiltAndroidApp
class FragmjectApplication : Application(), ImageLoaderFactory {

    @Inject
    lateinit var webViewPool: WebViewPool

    @Inject
    @CoilOkHttpClient
    lateinit var okHttpClient: OkHttpClient

    override fun onCreate() {
        super.onCreate()

        // ===== 自定义 SSL 证书接入示例（默认关闭，需要时取消注释） =====
        // 证书文件放在 assets 或 res/raw 下，按需读取后注入即可；
        // 至少配置服务端证书（serverCertificates）或客户端证书（clientCertificate+密码）之一才会启用，
        // 未配置时自动回落到系统默认 SSL 校验，无性能损耗。
        //
        // 场景一：单向 TLS（固定服务端证书，替换默认 CA 校验）
        // OkUtils.setSslConfig(
        //     serverCertificates = arrayOf(assets.open("server.crt")),
        // )
        //
        // 场景二：双向 TLS（同时校验客户端证书）
        // OkUtils.setSslConfig(
        //     clientCertificate = assets.open("client.p12"),
        //     clientCertificatePwd = "your_password",
        //     serverCertificates = arrayOf(assets.open("server.crt")),
        // )
        // ===== 示例结束 =====
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(applicationContext)
            .crossfade(true)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .memoryCache {
                MemoryCache.Builder(applicationContext)
                    .maxSizePercent(0.10)
                    .build()
            }
            .diskCachePolicy(CachePolicy.ENABLED)
            .diskCache {
                DiskCache.Builder()
                    .directory(
                        CacheUtils.getDirFile(
                            applicationContext, "coil"
                        )
                    )
                    .maxSizeBytes(50L * 1024 * 1024)
                    .build()
            }
            .okHttpClient(okHttpClient)
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
     * WebView 单实例约 30~80MB，keep-alive 池上限为 8 个，在低端机上前台峰值可达数百 MB。
     * 主动响应 onTrimMemory 可在系统承压前释放资源，比被动等待 GC/被 kill 友好得多。
     */
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        when (level) {
            // UI 完全隐藏：用户切到后台/锁屏；保留 spare，清空 keep-alive。
            TRIM_MEMORY_UI_HIDDEN -> webViewPool.trimToSpare()
            // 进程仍在前台，但系统内存紧张：先做温和回收。
            TRIM_MEMORY_RUNNING_MODERATE,
            TRIM_MEMORY_RUNNING_LOW -> webViewPool.trimToSpare()
            // 进程已进入后台 LRU，且系统内存严重不足：彻底释放，争取不被 kill。
            TRIM_MEMORY_RUNNING_CRITICAL,
            TRIM_MEMORY_BACKGROUND,
            TRIM_MEMORY_MODERATE,
            TRIM_MEMORY_COMPLETE -> webViewPool.releaseAll()

            else -> Unit
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
        // 老接口的极端兜底：直接释放全部 WebView。
        webViewPool.releaseAll()
    }

}