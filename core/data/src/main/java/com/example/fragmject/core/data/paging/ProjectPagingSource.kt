package com.example.fragmject.core.data.paging

import com.example.fragmject.core.model.Article
import com.example.fragmject.core.network.datasource.ProjectDataSource

/**
 * 项目文章分页源（page 从 1 开始，按 cid 区分项目分类）。
 */
class ProjectPagingSource(
    private val cid: String,
    private val remote: ProjectDataSource,
) : BasePagingSource<Article>(startPage = 1) {

    override suspend fun fetchPage(page: Int): PageData<Article> {
        val data = remote.getProjectList(page, cid).data
        return PageData(
            items = data?.datas.orEmpty(),
            over = data?.over == true,
        )
    }
}
