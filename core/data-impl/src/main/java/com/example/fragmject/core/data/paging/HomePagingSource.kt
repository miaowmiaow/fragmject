package com.example.fragmject.core.data.impl.paging

import com.example.fragmject.core.model.Article
import com.example.fragmject.core.data.contract.remote.ArticleRemoteDataSource

/**
 * 首页文章分页源（page 从 0 开始）。
 */
class HomePagingSource(
    private val remote: ArticleRemoteDataSource,
) : BasePagingSource<Article>(startPage = 0) {

    override suspend fun fetchPage(page: Int): PageData<Article> {
        val data = remote.getArticleList(page).data
        return PageData(
            items = data?.datas.orEmpty(),
            over = data?.over == true,
        )
    }
}
