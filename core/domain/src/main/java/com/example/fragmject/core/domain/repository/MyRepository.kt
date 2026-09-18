package com.example.fragmject.core.domain.repository

import com.example.fragmject.core.domain.result.CollectResult
import com.example.fragmject.core.domain.result.MyCoinNextResult
import com.example.fragmject.core.domain.result.MyShareResult
import com.example.fragmject.core.domain.result.ShareArticleResult
import com.example.fragmject.core.model.Coin

/**
 * 当前登录用户「我的」领域端口：积分、分享、收藏。
 *
 * 实现由 data 层适配器提供，内部消化 [com.example.fragmject.core.network.http.DataResponse]。
 */
interface MyRepository {
    /** 我的积分汇总。 */
    suspend fun getUserCoin(): Coin?

    /** 我的积分明细（page 从 1 开始）。 */
    suspend fun getMyCoinList(page: Int): MyCoinNextResult

    /** 我分享的文章（page 从 1 开始）。 */
    suspend fun getMyShareList(page: Int): MyShareResult

    /** 新增一篇分享。 */
    suspend fun shareArticle(title: String, link: String): ShareArticleResult

    /** 收藏一篇文章（id 为文章 originId）。 */
    suspend fun collectArticle(id: String): CollectResult

    /** 取消收藏一篇文章（id 为文章 originId）。 */
    suspend fun uncollectArticle(id: String): CollectResult
}
