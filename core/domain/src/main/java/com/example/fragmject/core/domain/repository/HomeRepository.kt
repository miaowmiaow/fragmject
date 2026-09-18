package com.example.fragmject.core.domain.repository

import com.example.fragmject.core.domain.result.DomainResult
import com.example.fragmject.core.domain.result.PageData
import com.example.fragmject.core.model.Article
import com.example.fragmject.core.model.Banner
import kotlinx.coroutines.flow.Flow

/**
 * 首页聚合层领域端口：Room 唯一数据源模式。
 */
interface HomeRepository {
    fun observeHomeArticles(): Flow<List<Article>>
    fun observeHomeBanners(): Flow<List<Banner>>
    suspend fun refreshHome(): DomainResult<Int>
    suspend fun loadNextPage(page: Int): DomainResult<PageData>
}
