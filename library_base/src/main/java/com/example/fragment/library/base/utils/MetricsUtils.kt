package com.example.fragment.library.base.utils

import android.content.Context
import com.example.fragment.library.base.component.provider.BaseProvider

object MetricsUtils {
    /**
     * 获取屏幕宽度
     */
    val screenWidth: Int
        get() = getScreenWidth(BaseProvider.mContext)

    fun getScreenWidth(context: Context): Int {
        val metrics = context.resources.displayMetrics
        return metrics.widthPixels
    }

    /**
     * 获取屏幕高度
     */
    val screenHeight: Int
        get() = getScreenHeight(BaseProvider.mContext)

    fun getScreenHeight(context: Context): Int {
        val metrics = context.resources.displayMetrics
        return metrics.heightPixels
    }

    val densityDpi: Int
        get() = getDensityDpi(BaseProvider.mContext)

    fun getDensityDpi(context: Context): Int {
        return context.resources.displayMetrics.densityDpi
    }

    /**
     * dp转px
     */
    fun dp2px(dp: Float): Float {
        return dp2px(BaseProvider.mContext, dp)
    }

    fun dp2px(context: Context, dp: Float): Float {
        val scale = context.resources.displayMetrics.density
        return (dp * scale + 0.5f)
    }

    /**
     * px转dp
     */
    fun px2dp(px: Float): Float {
        return px2dp(BaseProvider.mContext, px)
    }

    fun px2dp(context: Context, px: Float): Float {
        val scale = context.resources.displayMetrics.density
        return (px / scale + 0.5f)
    }

    /**
     * px转sp
     */
    fun px2sp(px: Float): Float {
        return px2sp(BaseProvider.mContext, px)
    }

    fun px2sp(context: Context, px: Float): Float {
        val fontScale = context.resources.displayMetrics.scaledDensity
        return (px / fontScale + 0.5f)
    }

    /**
     * sp转px
     */
    fun sp2px(sp: Float): Float {
        return sp2px(BaseProvider.mContext, sp)
    }

    fun sp2px(context: Context, sp: Float): Float {
        val fontScale = context.resources.displayMetrics.scaledDensity
        return (sp * fontScale + 0.5f)
    }
}