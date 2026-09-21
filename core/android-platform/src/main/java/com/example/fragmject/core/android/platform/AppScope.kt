package com.example.fragmject.core.android.platform

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * 未捕获异常的外部日志钩子。
 *
 * 默认（null）时回退 [Throwable.printStackTrace] 输出到标准错误。app 层如需接入
 * 统一日志范式，可在启动时赋值为 `{ Log.e("AppScope", "Uncaught exception", it) }`。
 */
var appScopeExceptionLogger: ((Throwable) -> Unit)? = null

/**
 * 应用级协程作用域：
 * - 使用 [SupervisorJob] 保证子协程之间互不影响
 * - 默认运行在 [Dispatchers.IO]
 * - 携带统一的异常处理，避免未捕获异常导致进程崩溃
 *
 * 适用于"生命周期等同进程"的轻量后台任务（如缓存清理、磁盘 IO、进程级单例状态等），
 * 而非组件级任务（组件级请使用 viewModelScope/lifecycleScope）。
 *
 * 严禁使用 GlobalScope 或 CoroutineScope(Dispatchers.IO).launch 这种"即用即抛"的写法。
 */
object AppScope : CoroutineScope by CoroutineScope(
    SupervisorJob() +
            Dispatchers.IO +
            CoroutineExceptionHandler { _, throwable ->
                appScopeExceptionLogger?.invoke(throwable) ?: throwable.printStackTrace()
            }
)
