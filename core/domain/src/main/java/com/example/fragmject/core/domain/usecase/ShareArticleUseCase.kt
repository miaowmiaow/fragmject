package com.example.fragmject.core.domain.usecase

import com.example.fragmject.core.domain.repository.MyRepository
import com.example.fragmject.core.domain.result.ShareArticleResult
import javax.inject.Inject

/**
 * 提交一篇分享文章。
 */
class ShareArticleUseCase @Inject constructor(
    private val repo: MyRepository,
) {
    suspend operator fun invoke(title: String, link: String): ShareArticleResult {
        return repo.shareArticle(title, link)
    }
}