package com.example.fragmject.core.domain.repository

import com.example.fragmject.core.domain.result.DomainResult
import com.example.fragmject.core.domain.result.PageData
import com.example.fragmject.core.model.Article
import kotlinx.coroutines.flow.Flow

/**
 * 我的收藏聚合层领域端口：Room 唯一数据源模式。
 */
interface MyCollectRepository {
    fun observeMyCollect(): Flow<List<Article>>
    suspend fun refreshMyCollect(): DomainResult<Int>
    suspend fun loadMyCollectNextPage(page: Int): DomainResult<PageData>
}
