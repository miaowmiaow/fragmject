package com.example.fragmject.core.domain.usecase

import com.example.fragmject.core.data.repository.MyRepository
import javax.inject.Inject

/**
 * 提交一篇分享文章。
 */
class ShareArticleUseCase @Inject constructor(
    private val repo: MyRepository,
) {
    suspend operator fun invoke(title: String, link: String): ShareArticleResult {
        val response = repo.shareArticle(title, link)
        return ShareArticleResult(
            success = response.errorCode == "0",
            message = response.errorMsg,
        )
    }
}

data class ShareArticleResult(
    val success: Boolean,
    val message: String,
)