package com.example.miaow.base.bus

import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import java.util.concurrent.ConcurrentHashMap

/**
 * SharedFlowBus 只会将更新通知给活跃的观察者，
 */
object SharedFlowBus {

    private val events = ConcurrentHashMap<Class<*>, MutableSharedFlow<Any>>()
    private val stickyEvents = ConcurrentHashMap<Class<*>, MutableSharedFlow<Any>>()

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> with(key: Class<T>): MutableSharedFlow<T> {
        // 使用 getOrPut 保证 containsKey + put 的原子语义，避免高并发下重复创建 / 丢事件
        return events.getOrPut(key) {
            MutableSharedFlow(0, 1, BufferOverflow.DROP_OLDEST)
        } as MutableSharedFlow<T>
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> withSticky(key: Class<T>): MutableSharedFlow<T> {
        return stickyEvents.getOrPut(key) {
            MutableSharedFlow(1, 1, BufferOverflow.DROP_OLDEST)
        } as MutableSharedFlow<T>
    }

    fun <T : Any> on(key: Class<T>): LiveData<T> {
        return with(key).asLiveData()
    }

    fun <T : Any> onSticky(key: Class<T>): LiveData<T> {
        return withSticky(key).asLiveData()
    }

}