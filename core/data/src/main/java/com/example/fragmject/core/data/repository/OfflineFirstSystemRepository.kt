package com.example.fragmject.core.data.repository

import com.example.fragmject.core.network.datasource.ArticleDataSource

import com.example.fragmject.core.database.dao.ArticleDao
import com.example.fragmject.core.database.model.toDomain
import com.example.fragmject.core.database.model.toEntity
import com.example.fragmject.core.data.util.fetchAsDomainResult
import com.example.fragmject.core.domain.repository.SystemRepository
import com.example.fragmject.core.domain.result.DomainResult
import com.example.fragmject.core.domain.result.PageData
import com.example.fragmject.core.model.Article
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 体系文章 Repository：第 0 页进 Room 缓存，后续页走 ViewModel 内存。
 *
 * 体系接口 page 从 0 开始。
 */
@Singleton
class OfflineFirstSystemRepository @Inject constructor(
    private val articleDao: ArticleDao,
    private val articleRepo: ArticleDataSource,
) : SystemRepository {

    companion object {
        const val CACHE_KEY_PREFIX = "system"
    }

    // ===== 观察 Room（仅第 0 页） =====

    /** 只观察第 0 页（system_{cid}_0）。 */
    override fun observeSystemArticles(cid: String): Flow<List<Article>> =
        articleDao.getByCacheKey("${CACHE_KEY_PREFIX}_${cid}_0").map { entities ->
            entities.map { it.toDomain() }
        }

    // ===== 网络 → Room 写入 / 直接返回 =====

    /** 刷新第 0 页：写 Room → Flow 自动推送。返回 DomainResult 携带 pageCount。 */
    override suspend fun refreshSystemArticles(cid: String): DomainResult<Int> {
        return fetchAsDomainResult(
            call = { articleRepo.getArticleListByCid(0, cid) },
        ) { resp ->
            val datas = resp.data?.datas.orEmpty()
            if (datas.isNotEmpty()) {
                val cacheKey = "${CACHE_KEY_PREFIX}_${cid}_0"
                articleDao.replaceAll(
                    cacheKey,
                    datas.mapIndexed { i, article -> article.toEntity(cacheKey, i) })
            }
            resp.data?.pageCount?.toIntOrNull() ?: 0
        }
    }

    /** 加载第 N 页（N>=1）：仅网络，不写 Room。 */
    override suspend fun loadSystemNextPage(cid: String, page: Int): DomainResult<PageData> {
        return fetchAsDomainResult(
            call = { articleRepo.getArticleListByCid(page, cid) },
        ) { resp ->
            PageData(
                articles = resp.data?.datas.orEmpty(),
                pageCount = resp.data?.pageCount?.toIntOrNull(),
            )
        }
    }
}