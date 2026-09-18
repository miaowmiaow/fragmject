package com.example.fragmject.core.domain.repository

import com.example.fragmject.core.domain.result.DomainResult
import com.example.fragmject.core.model.ShareArticle

/**
 * 用户中心聚合层领域端口。
 */
interface UserCenterRepository {
    suspend fun getUserShareArticles(userId: String, page: Int): DomainResult<ShareArticle>
}
