package com.example.fragmject.core.data.repository.paging

import com.example.fragmject.core.model.Article
import com.example.fragmject.core.data.contract.remote.ArticleRemoteDataSource

/**
 * 首页文章分页源（page 从 0 开始）。
 *
 * @param prefetcher 进程启动时发起的第一页预取；为 null（如单测）时完全走原网络路径。
 */
class HomePagingSource(
    private val remote: ArticleRemoteDataSource,
    private val prefetcher: HomeFirstPagePrefetcher? = null,
) : BasePagingSource<Article>(startPage = 0) {

    override suspend fun fetchPage(page: Int): PageData<Article> {
        // 第 0 页优先消费 Application 启动时发起的预取，把网络等待与 Activity 启动重叠；
        // 后续分页与「预取未命中」都落到下面的直接请求，行为与改造前一致。
        if (page == 0 && prefetcher != null) {
            return prefetcher.awaitFirstPage()
        }
        val data = remote.getArticleList(page).requirePagingData()
        return PageData(
            items = data.datas.requirePagingField("data.datas"),
            over = data.over,
        )
    }
}
