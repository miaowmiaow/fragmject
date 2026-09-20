package com.example.fragmject.core.data.paging

import com.example.fragmject.core.model.Article
import com.example.fragmject.core.network.datasource.MyRemoteDataSource

/**
 * 我的分享文章分页源（page 从 0 开始）。
 */
class MySharePagingSource(
    private val remote: MyRemoteDataSource,
) : BasePagingSource<Article>(startPage = 0) {

    override suspend fun fetchPage(page: Int): PageData<Article> {
        val data = remote.getMyShareList(page).data?.shareArticles
        return PageData(
            items = data?.datas.orEmpty(),
            over = data?.over == true,
        )
    }
}
