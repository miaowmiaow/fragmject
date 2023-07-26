package com.example.miaow.base.utils

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/**
 * 倒计时
 */
@ExperimentalCoroutinesApi
fun countDownCoroutines(
    total: Int,
    onTick: (Int) -> Unit,
    onFinish: () -> Unit,
    scope: CoroutineScope = GlobalScope
): Job {
    return flow {
        for (i in total downTo 0) {
            emit(i)
            delay(1000)
        }
    }.flowOn(Dispatchers.Default) //flowOn只影响该运算符之前的CoroutineContext，对它之后的CoroutineContext没有任何影响
        .onEach { onTick.invoke(it) } //每个值释放的时候可以执行的一段代码
        .onCompletion { onFinish.invoke() } //最后一个值释放完成之后被执行
        .catch { e -> println(e) } //catch函数能够捕获之前产生的异常，之后的异常无法捕获
        .flowOn(Dispatchers.Main)
        .launchIn(scope) //scope.launch { flow.collect() }的缩写, 代表在某个协程上下文环境中去接收释放的值
}
