package com.example.fragment.library.base.utils

import android.os.Handler
import android.os.Looper
import java.util.concurrent.Executor


class MainThreadExecutor private constructor() : Executor {

    private val handler = Handler(Looper.getMainLooper())

    override fun execute(runnable: Runnable) {
        handler.post(runnable)
    }

    companion object {

        private val INSTANCE = MainThreadExecutor()

        fun get(): MainThreadExecutor {
            return INSTANCE
        }
    }
}
