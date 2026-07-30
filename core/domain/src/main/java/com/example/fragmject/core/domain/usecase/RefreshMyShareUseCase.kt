package com.example.fragmject.core.domain.usecase

import com.example.fragmject.core.data.repository.MyRepository
import com.example.fragmject.core.model.Article
import javax.inject.Inject

/**
 * 加载我分享的文章列表（刷新用，page 从 0 开始）。
 */
class RefreshMyShareUseCase @Inject constructor(
    private val repo: MyRepository,
) {
    suspend operator fun invoke(page: Int): MyShareResult {
        val response = repo.getMyShareList(page)
        val data = response.data?.shareArticles
        val datas = data?.datas.orEmpty()
        return MyShareResult(
            articles = datas,
            pageCount = data?.pageCount?.toInt(),
            hasMore = datas.isNotEmpty(),
        )
    }
}

data class MyShareResult(
    val articles: List<Article> = emptyList(),
    val pageCount: Int? = null,
    val hasMore: Boolean = false,
)