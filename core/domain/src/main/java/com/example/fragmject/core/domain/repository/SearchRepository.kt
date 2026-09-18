package com.example.fragmject.core.domain.repository

import com.example.fragmject.core.domain.result.ArticlePageResult
import com.example.fragmject.core.domain.result.DomainResult
import com.example.fragmject.core.model.HotKey
import kotlinx.coroutines.flow.Flow

/**
 * 搜索聚合层领域端口：热搜词（Room 唯一数据源）+ 搜索结果。
 */
interface SearchRepository {
    fun observeHotKey(): Flow<List<HotKey>>
    suspend fun refreshHotKey(): DomainResult<Unit>
    suspend fun searchArticles(key: String, page: Int): DomainResult<ArticlePageResult>
}
