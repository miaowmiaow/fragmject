package com.example.fragmject.core.domain.repository

import com.example.fragmject.core.domain.result.DomainResult
import com.example.fragmject.core.domain.result.PageData
import com.example.fragmject.core.model.Article
import com.example.fragmject.core.model.ProjectTree
import kotlinx.coroutines.flow.Flow

/**
 * 项目领域端口：项目树 + 项目分类文章（Room 唯一数据源模式，第 1 页进 Room）。
 */
interface ProjectRepository {
    fun observeProjectTree(): Flow<List<ProjectTree>>
    fun observeProjectArticles(cid: String): Flow<List<Article>>
    suspend fun refreshProjectTree(): DomainResult<Unit>
    suspend fun refreshProjectArticles(cid: String): DomainResult<Int>
    suspend fun loadProjectNextPage(cid: String, page: Int): DomainResult<PageData>
}
