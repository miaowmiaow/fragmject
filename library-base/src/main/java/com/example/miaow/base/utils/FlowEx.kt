package com.example.miaow.base.utils

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach

/**
 * 倒计时
 *
 * @param scope 必须由调用方提供（如 viewModelScope / lifecycleScope），
 * 避免使用 GlobalScope 导致协程脱离组件生命周期，造成内存泄漏。
 */
@ExperimentalCoroutinesApi
fun countDownCoroutines(
    total: Int,
    scope: CoroutineScope,
    onTick: (Int) -> Unit,
    onFinish: () -> Unit,
): Job {
    return flow {
        for (i in total downTo 0) {
            emit(i)
            delay(1000)
        }
    }.flowOn(Dispatchers.Default) //flowOn只影响该运算符之前的CoroutineContext，对它之后的CoroutineContext没有任何影响
        .onEach { onTick.invoke(it) } //每个值释放的时候可以执行的一段代码
        .onCompletion { onFinish.invoke() } //最后一个值释放完成之后被执行
        .catch { e -> Log.e("countDownCoroutines", "countdown failed", e) } //catch函数能够捕获之前产生的异常，之后的异常无法捕获
        .flowOn(Dispatchers.Main)
        .launchIn(scope) //scope.launch { flow.collect() }的缩写, 代表在某个协程上下文环境中去接收释放的值
}
