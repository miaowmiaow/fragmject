package com.example.fragmject.core.domain.usecase

import com.example.fragmject.core.domain.repository.UserCenterRepository
import com.example.fragmject.core.domain.result.DomainResult
import com.example.fragmject.core.domain.result.UserShareResult
import javax.inject.Inject

/**
 * 获取用户分享文章（分页）。
 * 将 [DomainResult] 包装为 [UserShareResult]。
 */
class GetUserShareArticlesUseCase @Inject constructor(
    private val repo: UserCenterRepository,
) {
    suspend operator fun invoke(userId: String, page: Int): UserShareResult {
        return when (val result = repo.getUserShareArticles(userId, page)) {
            is DomainResult.Success -> {
                val data = result.data
                UserShareResult(
                    coin = data.coinInfo,
                    articles = data.shareArticles?.datas.orEmpty(),
                    pageCount = data.shareArticles?.pageCount?.toInt(),
                )
            }

            is DomainResult.Failure -> UserShareResult(articles = emptyList(), pageCount = null)
        }
    }
}