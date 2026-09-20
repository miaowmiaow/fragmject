package com.example.fragmject.core.domain.repository

import androidx.paging.PagingData
import com.example.fragmject.core.model.Article
import com.example.fragmject.core.model.Coin
import kotlinx.coroutines.flow.Flow

/**
 * 用户中心聚合层领域端口。
 */
interface UserCenterRepository {
    /** 用户公开信息（头像/昵称/积分）。 */
    suspend fun getUserCoin(userId: String): Coin?

    /** 用户分享文章分页数据流（page 从 1 开始）。 */
    fun getUserSharePagingData(userId: String): Flow<PagingData<Article>>
}
