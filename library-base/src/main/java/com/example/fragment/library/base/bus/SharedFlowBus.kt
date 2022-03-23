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
    private var stickyEvents = ConcurrentHashMap<Any, MutableSharedFlow<Any>>()

    fun <T> with(key: Class<T>): MutableSharedFlow<T> {
        if (!events.containsKey(key)) {
            events[key] = MutableSharedFlow(0, 1, BufferOverflow.DROP_OLDEST)
        }
        return events[key] as MutableSharedFlow<T>
    }

    fun <T> withSticky(key: Class<T>): MutableSharedFlow<T> {
        if (!stickyEvents.containsKey(key)) {
            stickyEvents[key] = MutableSharedFlow(1, 1, BufferOverflow.DROP_OLDEST)
        }
        return stickyEvents[key] as MutableSharedFlow<T>
    }

    fun <T> on(key: Class<T>): LiveData<T> {
        return with(key).asLiveData()
    }

    fun <T> onSticky(key: Class<T>): LiveData<T> {
        return withSticky(key).asLiveData()
    }

}