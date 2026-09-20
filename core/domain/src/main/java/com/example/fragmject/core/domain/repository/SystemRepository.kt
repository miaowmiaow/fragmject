package com.example.fragmject.core.domain.repository

import androidx.paging.PagingData
import com.example.fragmject.core.model.Article
import kotlinx.coroutines.flow.Flow

/**
 * 体系文章领域端口：纯网络分页。
 */
interface SystemRepository {
    fun getSystemPagingData(cid: String): Flow<PagingData<Article>>
}
