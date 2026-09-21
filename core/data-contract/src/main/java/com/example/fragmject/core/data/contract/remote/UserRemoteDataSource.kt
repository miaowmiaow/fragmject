package com.example.fragmject.core.data.contract.remote

import com.example.fragmject.core.data.contract.model.DataResponse
import com.example.fragmject.core.data.contract.model.HttpResponse
import com.example.fragmject.core.model.ShareArticle
import com.example.fragmject.core.model.User

/**
 * 用户远程数据源：登录 / 注册 / 登出 / 用户分享。
 *
 * 定义在 data-contract 层，由 core:network 的 Retrofit 接口（internal UserService）
 * 继承并实现；core:data 的 Repository 只依赖本契约，不感知 Retrofit。
 */
interface UserRemoteDataSource {
    suspend fun login(username: String, password: String): DataResponse<User>
    suspend fun register(username: String, password: String, repassword: String): DataResponse<User>
    suspend fun logout(): HttpResponse
    suspend fun getUserShareArticles(userId: String, page: Int): DataResponse<ShareArticle>
}
