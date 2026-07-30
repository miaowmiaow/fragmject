package com.example.fragmject.core.ui

import kotlin.math.abs

/**
 * 头像资源选择器。
 *
 * 原置于 [com.example.fragmject.core.model.Article]、
 * [com.example.fragmject.core.database.model.UserEntity]、[com.example.fragmject.core.model.Coin]
 * 中的 avatarId / avatar 属性已提取至此工具类，
 * 使数据模型不再依赖 Android 资源 ID，可安全移入 :data 模块。
 */
object AvatarHelper {

    private val avatarList = listOf(
        R.mipmap.avatar_1_raster,
        R.mipmap.avatar_2_raster,
        R.mipmap.avatar_3_raster,
        R.mipmap.avatar_4_raster,
        R.mipmap.avatar_5_raster,
        R.mipmap.avatar_6_raster,
    )

    fun avatarResId(userId: String): Int {
        return try {
            avatarList[abs(userId.toInt()) % avatarList.size]
        } catch (e: Exception) {
            R.mipmap.ic_launcher
        }
    }

    fun avatarResId(userId: Long): Int {
        return avatarResId(userId.toString())
    }
}
