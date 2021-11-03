package com.example.fragment.library.base.bus

import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.set

/**
 * SharedFlowBus 只会将更新通知给活跃的观察者，
 */
object SharedFlowBus {

    private var events = ConcurrentHashMap<Any, MutableSharedFlow<Any>>()

    fun <T> with(objectKey: Class<T>): MutableSharedFlow<T> {
        var result = events[objectKey]
        if (result == null) {
            result = MutableSharedFlow(1, 1, BufferOverflow.DROP_OLDEST)
            events[objectKey] = result
        }
        return result as MutableSharedFlow<T>
    }

    fun <T> on(objectKey: Class<T>): LiveData<T> {
        return with(objectKey).asLiveData()
    }

}