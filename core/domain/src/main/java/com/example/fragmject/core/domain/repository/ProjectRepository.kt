package com.example.fragmject.core.domain.repository

import androidx.paging.PagingData
import com.example.fragmject.core.domain.result.DomainResult
import com.example.fragmject.core.model.Article
import com.example.fragmject.core.model.ProjectTree
import kotlinx.coroutines.flow.Flow

/**
 * 项目领域端口：项目树（Tab 栏，Room 缓存）+ 项目分类文章（纯网络分页）。
 */
interface ProjectRepository {
    fun observeProjectTree(): Flow<List<ProjectTree>>
    suspend fun refreshProjectTree(): DomainResult<Unit>
    fun getProjectPagingData(cid: String): Flow<PagingData<Article>>
}
