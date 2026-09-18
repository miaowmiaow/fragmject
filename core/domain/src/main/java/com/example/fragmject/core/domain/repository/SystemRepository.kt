package com.example.fragmject.core.domain.repository

import com.example.fragmject.core.domain.result.DomainResult
import com.example.fragmject.core.domain.result.PageData
import com.example.fragmject.core.model.Article
import kotlinx.coroutines.flow.Flow

/**
 * 体系文章聚合层领域端口：Room 唯一数据源模式。
 */
interface SystemRepository {
    fun observeSystemArticles(cid: String): Flow<List<Article>>
    suspend fun refreshSystemArticles(cid: String): DomainResult<Int>
    suspend fun loadSystemNextPage(cid: String, page: Int): DomainResult<PageData>
}
