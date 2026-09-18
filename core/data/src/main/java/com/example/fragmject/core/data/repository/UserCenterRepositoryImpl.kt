package com.example.fragmject.core.data.repository

import com.example.fragmject.core.domain.repository.UserCenterRepository
import com.example.fragmject.core.domain.result.DomainResult
import com.example.fragmject.core.model.ShareArticle
import com.example.fragmject.core.network.datasource.UserRemoteDataSource
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [UserCenterRepository] 领域端口的 data 层适配器。
 */
@Singleton
class UserCenterRepositoryImpl @Inject constructor(
    private val userRemoteDataSource: UserRemoteDataSource,
) : UserCenterRepository {

    override suspend fun getUserShareArticles(
        userId: String,
        page: Int
    ): DomainResult<ShareArticle> {
        return runCatching { userRemoteDataSource.getUserShareArticles(userId, page) }
            .fold(
                onSuccess = { resp ->
                    val shareArticle = resp.data
                    if (resp.errorCode == "0" && shareArticle != null) {
                        DomainResult.Success(shareArticle)
                    } else {
                        DomainResult.Failure(
                            code = resp.errorCode.ifBlank { DomainResult.ERROR_UNKNOWN },
                            message = resp.errorMsg.ifBlank { "请求失败" },
                        )
                    }
                },
                onFailure = {
                    DomainResult.Failure(
                        code = DomainResult.ERROR_UNKNOWN,
                        message = it.message ?: "请求失败",
                    )
                },
            )
    }
}
