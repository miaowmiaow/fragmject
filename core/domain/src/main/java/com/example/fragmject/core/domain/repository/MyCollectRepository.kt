package com.example.fragmject.core.domain.repository

import androidx.paging.PagingData
import com.example.fragmject.core.model.Article
import kotlinx.coroutines.flow.Flow

/**
 * 我的收藏领域端口：纯网络分页。
 */
interface MyCollectRepository {
    fun getMyCollectPagingData(): Flow<PagingData<Article>>
}
