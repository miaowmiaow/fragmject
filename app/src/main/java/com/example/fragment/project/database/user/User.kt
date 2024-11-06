package com.example.fragment.project.database.user

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import com.example.fragment.project.R
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import kotlin.math.abs

@Entity(primaryKeys = ["unique_id"])
@Parcelize
data class User @JvmOverloads constructor(
    @ColumnInfo(name = "unique_id") var uniqueId: Long = 0,
    @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "username") val username: String,
    @ColumnInfo(name = "nickname") val nickname: String,
    @ColumnInfo(name = "dark_theme") var darkTheme: Boolean,
    @Ignore val token: String = "",
    @Ignore val password: String = "",
    @Ignore val admin: String = "",
    @Ignore val email: String = "",
    @Ignore val icon: String = "",
    @Ignore val type: String = "",
    @Ignore val publicName: String = "",
    @Ignore val coinCount: String = "",
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