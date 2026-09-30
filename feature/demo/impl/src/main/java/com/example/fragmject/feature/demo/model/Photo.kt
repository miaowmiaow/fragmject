package com.example.fragmject.feature.demo.model

import android.util.Log
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
        var index = 0
        try {
            if (id >= 0) {
                index = id % 6
            }
        } catch (e: Exception) {
            Log.e("GridSelectScreen", "getAvatarRes: id=$id failed", e)
        }
        return avatarList[index]
    }
}
