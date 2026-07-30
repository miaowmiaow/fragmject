package com.example.fragmject.core.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * 登录/注册等 HTTP 响应中返回的用户信息 DTO，无 Room 依赖。
 *
 * 与 [com.example.fragmject.core.database.model.UserEntity]（Room Entity）对应，
 * 转换由 [com.example.fragmject.core.database.store.UserStore] 处理。
 */
@Parcelize
data class User @JvmOverloads constructor(
    val id: Long = 0,
    val username: String? = null,
    val nickname: String? = null,
    val token: String? = null,
    val password: String? = null,
    val admin: String? = null,
    val email: String? = null,
    val icon: String? = null,
    val type: String? = null,
    val publicName: String? = null,
    val coinCount: String? = null,
    val collectIds: List<Int>? = null,
) : Parcelable
