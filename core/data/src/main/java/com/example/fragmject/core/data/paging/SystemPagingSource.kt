package com.example.fragmject.core.data.paging

import com.example.fragmject.core.model.Article
import com.example.fragmject.core.network.datasource.ArticleDataSource

/**
 * 体系文章分页源（page 从 0 开始，按 cid 区分体系分类）。
 */
class SystemPagingSource(
    private val cid: String,
    private val remote: ArticleDataSource,
) : BasePagingSource<Article>(startPage = 0) {

    override suspend fun fetchPage(page: Int): PageData<Article> {
        val data = remote.getArticleListByCid(page, cid).data
        return PageData(
            items = data?.datas.orEmpty(),
            over = data?.over == true,
        )
    }
}
