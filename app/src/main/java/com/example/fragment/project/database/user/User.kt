package com.example.fragment.project.database.user

import android.os.Parcelable
import android.util.Log
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import com.example.fragment.project.R
import com.example.miaow.base.http.HttpResponse
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import kotlin.math.abs

data class Login(
    val data: User? = null
) : HttpResponse()

data class Register(
    val data: User? = null
) : HttpResponse()

@Entity(primaryKeys = ["id", "username"])
@Parcelize
data class User @JvmOverloads constructor(
    @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "username") val username: String,
    @ColumnInfo(name = "nickname") val nickname: String,
    @Ignore val token: String = "",
    @Ignore val password: String = "",
    @Ignore val admin: String = "",
    @Ignore val email: String = "",
    @Ignore val icon: String = "",
    @Ignore val type: String = "",
    @Ignore val publicName: String = "",
    @Ignore var coinCount: String = "",
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