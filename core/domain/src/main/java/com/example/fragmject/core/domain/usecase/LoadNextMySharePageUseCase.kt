package com.example.fragmject.core.domain.usecase

import com.example.fragmject.core.data.repository.MyRepository
import javax.inject.Inject

/**
 * 加载我分享的文章下一页。复用 [MyShareResult]。
 */
class LoadNextMySharePageUseCase @Inject constructor(
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