package com.example.fragmject.core.data.paging

import com.example.fragmject.core.model.Article
import com.example.fragmject.core.network.datasource.UserRemoteDataSource

/**
 * 用户分享文章分页源（page 从 1 开始）。
 */
class UserSharePagingSource(
    private val userId: String,
    private val remote: UserRemoteDataSource,
) : BasePagingSource<Article>(startPage = 1) {

    override suspend fun fetchPage(page: Int): PageData<Article> {
        val data = remote.getUserShareArticles(userId, page).data?.shareArticles
        return PageData(
            items = data?.datas.orEmpty(),
            over = data?.over == true,
        )
    }
}
