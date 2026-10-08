package com.example.fragmject.core.android.platform.app

import android.util.Log
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 进程级协程作用域（Hilt 单例）。
 *
 * 适用场景：生命周期等同进程的轻量后台任务——缓存清理、磁盘 IO、进程级单例状态订阅等。
 * 组件级任务请使用 `viewModelScope` / `lifecycleScope`，不要用本作用域承载页面相关工作。
 *
 * 设计为 `@Inject constructor` 的 `@Singleton` 类，而非 `object`：
 * - 消费方通过构造注入获取，单测可用 `TestScope` 替换，不必依赖全局单例；
 * - 无需 `@Module`：Hilt 对带 `@Inject constructor` 的类自动生成工厂，
 *   不存在「库模块里的 @InstallIn 模块能否被 :app 发现」的不确定性；
 * - 异常不再回退 `printStackTrace`（release 下等于丢弃），统一走 [Log.e]。
 *
 * 严禁使用 `GlobalScope` 或 `CoroutineScope(Dispatchers.IO).launch` 这类即用即抛写法。
 */
@Singleton
class AppCoroutineScope @Inject constructor() : CoroutineScope {

    override val coroutineContext = SupervisorJob() +
        Dispatchers.IO +
        CoroutineExceptionHandler { _, throwable ->
            Log.e(TAG, "uncaught exception in app scope", throwable)
        }

    private companion object {
        const val TAG = "AppCoroutineScope"
    }
}
