package com.example.fragmject.core.data.impl.paging

import com.example.fragmject.core.model.Article
import com.example.fragmject.core.data.contract.remote.ArticleRemoteDataSource

/**
 * 搜索文章分页源（page 从 0 开始）。
 */
class SearchPagingSource(
    private val key: String,
    private val remote: ArticleRemoteDataSource,
) : BasePagingSource<Article>(startPage = 0) {

    override suspend fun fetchPage(page: Int): PageData<Article> {
        val data = remote.searchArticles(key, page).data
        return PageData(
            items = data?.datas.orEmpty(),
            over = data?.over == true,
        )
    }
}
