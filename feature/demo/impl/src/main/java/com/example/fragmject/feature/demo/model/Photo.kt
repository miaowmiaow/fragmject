package com.example.fragmject.feature.demo.model

import com.example.fragmject.core.ui.R

class Photo(
    val id: Int
) {
    private val avatarList: List<Int> = listOf(
        R.mipmap.avatar_1_raster,
        R.mipmap.avatar_2_raster,
        R.mipmap.avatar_3_raster,
        R.mipmap.avatar_4_raster,
        R.mipmap.avatar_5_raster,
        R.mipmap.avatar_6_raster,
    )

    fun getAvatarRes(): Int {
        val index = if (id >= 0) id % 6 else 0
        return avatarList[index]
    }
}
