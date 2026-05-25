package com.example.miaow.base.utils

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import android.util.Log

/**
 * 应用级协程作用域：
 * - 使用 [SupervisorJob] 保证子协程之间互不影响
 * - 默认运行在 [Dispatchers.IO]
 * - 携带统一的异常处理，避免未捕获异常导致进程崩溃
 *
 * 适用于"生命周期等同进程"的轻量后台任务（如缓存清理、磁盘 IO 等），
 * 而非组件级任务（组件级请使用 viewModelScope/lifecycleScope）。
 *
 * 严禁使用 GlobalScope 或 CoroutineScope(Dispatchers.IO).launch 这种"即用即抛"的写法。
 */
object AppScope : CoroutineScope by CoroutineScope(
    SupervisorJob() +
            Dispatchers.IO +
            CoroutineExceptionHandler { _, throwable ->
                Log.e("AppScope", "Uncaught exception", throwable)
            }
)
