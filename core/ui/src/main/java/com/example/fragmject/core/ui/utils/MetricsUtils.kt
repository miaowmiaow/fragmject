package com.example.fragmject.core.ui.utils

import android.content.Context

/**
 * 获取屏幕宽度
 */
fun Context.getScreenWidth(): Int {
    return resources.displayMetrics.widthPixels
}