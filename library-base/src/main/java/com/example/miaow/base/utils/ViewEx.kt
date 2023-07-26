package com.example.miaow.base.utils

import android.view.View

/**
 * 防抖动
 */
inline fun View.setThrottleListener(delayMillis: Long = 1000L, crossinline onClick: () -> Unit) {
    this.setOnClickListener {
        this.isClickable = false
        onClick()
        this.postDelayed({
            this.isClickable = true
        }, delayMillis)
    }
}