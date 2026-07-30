package com.example.fragmject.core.database.model

import android.os.Parcelable
import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Ignore
import androidx.room3.PrimaryKey
import kotlinx.parcelize.Parcelize

@Entity(tableName = "user")
@Parcelize
data class UserEntity @JvmOverloads constructor(
    @PrimaryKey val id: Long,
    @ColumnInfo(name = "username") var username: String? = null,
    @ColumnInfo(name = "nickname") var nickname: String? = null,
    @Ignore val token: String? = null,
    @Ignore val password: String? = null,
    @Ignore val admin: String? = null,
    @Ignore val email: String? = null,
    @ColumnInfo(name = "icon") val icon: String? = null,
    @Ignore val type: String? = null,
    @Ignore val publicName: String? = null,
    @Ignore val coinCount: String? = null,
    @Ignore val collectIds: List<Int>? = null,
) : Parcelable