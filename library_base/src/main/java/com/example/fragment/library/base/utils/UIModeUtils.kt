package com.example.fragment.library.base.utils

import android.content.Context
import android.content.res.Configuration

object UIModeUtils {

    fun Context.isNightMode(): Boolean {
        val config = resources.configuration
        val uiMode = config.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return uiMode == Configuration.UI_MODE_NIGHT_YES
    }

}