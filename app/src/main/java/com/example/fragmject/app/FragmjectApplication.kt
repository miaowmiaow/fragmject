package com.example.fragmject.app

import android.app.Application
import android.content.Context
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
import com.example.fragmject.app.security.AppSecurityGuard
import com.example.fragmject.core.webview.WebViewPool
import com.example.fragmject.core.domain.system.SystemStorage
// 登记过的架构例外（ArchitectureTest 规则 21 白名单）：app 组合根不应引用数据实现层，
// 但 Coil 需要 core:network 里「图片专用」OkHttpClient 的 qualifier。
// 彻底消除需把 ImageLoader 装配下沉到同时可见 coil 与 network 的 core 模块，勿在此新增同类引用。
import com.example.fragmject.core.network.di.ImageOkHttpClient
import dagger.hilt.android.HiltAndroidApp
import okhttp3.OkHttpClient
import javax.inject.Inject

@HiltAndroidApp
class FragmjectApplication : Application(), ImageLoaderFactory {

    /**
     * 尽早启动安全防护。
     *
     * ============================ 为什么选 attachBaseContext ============================
     * Application 的启动顺序是：
     *   attachBaseContext → onCreate → ContentProvider.onCreate → Activity.onCreate
     *
     * 安全防护必须在「任何业务代码/第三方 SDK 初始化」之前完成两件事：
     *   1. ptrace 占坑（先占住 tracer，后续调试器 attach 会失败）；
     *   2. 首轮检测（判断进程在启动窗口内是否已被附加/注入）。
     * 放到 onCreate 或更晚，就会给攻击者留出「启动窗口」；放到 ContentProvider 更晚，
     * 且 Hilt 等框架的 ContentProvider 可能已经做了大量初始化。
     *
     * ============================ 为什么这里用 this 是安全的 ============================
     * [attachBaseContext] 阶段 Application 实例已创建，但 Context 尚未完全 attach。
     * [AppSecurityGuard.start] 只做两件事会用到 Context：
     *   - 判断 `context is Application` 以注册 Activity 生命周期回调；
     *   - 异步签名校验时读取 PackageManager。
     * 两者都不依赖「资源已 attach」的前提，因此在此时调用是安全的。
     *
     * ============================ 代价与跳过 ============================
     * 首轮检测是**同步**执行的（含 libc CRC 与端口扫描，几十毫秒量级），
     * 会略微增加冷启动耗时；这是「尽早发现」与「启动速度」之间的有意取舍。
     * Debug 包会在 [AppSecurityGuard.start] 内部直接返回，不影响本地开发调试。
     */
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        AppSecurityGuard.start(this)
    }

    @Inject
    lateinit var webViewPool: WebViewPool

    @Inject
    lateinit var systemStorage: SystemStorage

    @Inject
    @ImageOkHttpClient
    lateinit var imageClient: OkHttpClient

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
                    .directory(systemStorage.cacheDirectory())
                    .maxSizeBytes(50L * 1024 * 1024)
                    .build()
            }
            .okHttpClient(imageClient)
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
    @Suppress("DEPRECATION")
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
