package com.example.fragmject.core.database.model

import android.os.Parcelable
import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Ignore
import androidx.room3.PrimaryKey
import kotlinx.parcelize.Parcelize

/**
 * 当前登录用户的本地缓存（单行表）。
 *
 * **`username` / `nickname` / `icon` 入库前已加密**（`v1:` 前缀 + Base64，见
 * `core.database.crypto.UserFieldCipher`），只能经 `UserLocalDataSourceImpl` 读写：
 * 直接读 DAO 拿到的是密文，直接写 DAO 会把明文落盘。
 * `token` / `password` 等字段标 `@Ignore`，本就不落库。
 */
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