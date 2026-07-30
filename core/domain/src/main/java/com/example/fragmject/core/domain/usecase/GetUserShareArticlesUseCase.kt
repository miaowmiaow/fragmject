package com.example.fragmject.core.domain.usecase

import com.example.fragmject.core.data.repository.UserCenterRepository
import com.example.fragmject.core.model.Coin
import com.example.fragmject.core.model.Article
import javax.inject.Inject

/**
 * 获取用户分享文章（分页）。
 * 将 [com.example.fragmject.core.common.result.AppResult] 包装为 [UserShareResult]。
 */
class GetUserShareArticlesUseCase @Inject constructor(
    private val repo: UserCenterRepository,
) {
    suspend operator fun invoke(userId: String, page: Int): UserShareResult {
        val response = repo.getUserShareArticles(userId, page)
        val data = response.getOrNull()?.data
        return if (response.isSuccess && data != null) {
            UserShareResult(
                coin = data.coinInfo,
                articles = data.shareArticles?.datas.orEmpty(),
                pageCount = data.shareArticles?.pageCount?.toInt(),
            )
        } else {
            UserShareResult(articles = emptyList(), pageCount = null)
        }
    }
}

data class UserShareResult(
    val coin: Coin? = null,
    val articles: List<Article> = emptyList(),
    val pageCount: Int? = null,
)