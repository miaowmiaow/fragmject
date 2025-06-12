package com.example.fragment.project.data

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.example.fragment.project.R
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import kotlin.math.abs

@Entity
@Parcelize
data class User @JvmOverloads constructor(
    @PrimaryKey val id: Long,
    @ColumnInfo(name = "username") var username: String = "",
    @ColumnInfo(name = "nickname") var nickname: String = "",
    @ColumnInfo(name = "dark_theme") var darkTheme: Boolean,
    @Ignore val token: String? = null,
    @Ignore val password: String? = null,
    @Ignore val admin: String? = null,
    @Ignore val email: String? = null,
    @Ignore val icon: String? = null,
    @Ignore val type: String? = null,
    @Ignore val publicName: String? = null,
    @Ignore val coinCount: String? = null,
    @Ignore val collectIds: List<Int>? = null,
) : Parcelable {

    @IgnoredOnParcel
    val avatar by lazy {
        try {
            listOf(
                R.mipmap.avatar_1_raster,
                R.mipmap.avatar_2_raster,
                R.mipmap.avatar_3_raster,
                R.mipmap.avatar_4_raster,
                R.mipmap.avatar_5_raster,
                R.mipmap.avatar_6_raster,
            )[abs(id.toInt()) % 6]
        } catch (e: Exception) {
            R.mipmap.ic_launcher
        }
    }

}