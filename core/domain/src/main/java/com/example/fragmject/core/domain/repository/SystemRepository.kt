package com.example.fragmject.core.domain.repository

import androidx.paging.PagingData
import com.example.fragmject.core.domain.result.DomainResult
import com.example.fragmject.core.model.Article
import com.example.fragmject.core.model.Tree
import kotlinx.coroutines.flow.Flow

/**
 * 体系领域端口：体系树（Room 唯一数据源）+ 体系文章（纯网络分页）。
 */
interface SystemRepository {
    fun observeSystemTree(): Flow<List<Tree>>
    suspend fun refreshSystemTree(): DomainResult<Unit>
    fun getSystemPagingData(cid: String): Flow<PagingData<Article>>
}
