package com.example.fragmject.core.data.repository

import com.example.fragmject.core.database.AppDatabase
import com.example.fragmject.core.database.model.toDomain
import com.example.fragmject.core.database.model.toEntity
import com.example.fragmject.core.model.Article
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * 体系文章 Repository：第 0 页进 Room 缓存，后续页走 ViewModel 内存。
 *
 * 体系接口 page 从 0 开始。
 */
class OfflineFirstSystemRepository(
    private val articleRepo: ArticleRepository,
) {

    companion object {
        const val CACHE_KEY_PREFIX = "system"
    }

    // ===== 观察 Room（仅第 0 页） =====

    /** 只观察第 0 页（system_{cid}_0）。 */
    fun observeSystemArticles(cid: String): Flow<List<Article>> =
        AppDatabase.getArticleDao().getByCacheKey("${CACHE_KEY_PREFIX}_${cid}_0").map { entities ->
            entities.map { it.toDomain() }
        }

    // ===== 网络 → Room 写入 / 直接返回 =====

    /** 刷新第 0 页：写 Room → Flow 自动推送。返回 pageCount。 */
    suspend fun refreshSystemArticles(cid: String): Int? {
        val articleList = runCatching { articleRepo.getArticleListByCid(cid, 0) }.getOrNull() ?: return null
        val datas = articleList.data?.datas ?: return null
        val cacheKey = "${CACHE_KEY_PREFIX}_${cid}_0"
        AppDatabase.getArticleDao().replaceAll(cacheKey,
            datas.mapIndexed { i, article -> article.toEntity(cacheKey, i) })
        return articleList.data?.pageCount?.toIntOrNull()
    }

    /** 加载第 N 页（N>=1）：仅网络，不写 Room。 */
    suspend fun loadSystemNextPage(cid: String, page: Int): PageData? {
        val articleList = runCatching { articleRepo.getArticleListByCid(cid, page) }.getOrNull() ?: return null
        val datas = articleList.data?.datas ?: return null
        if (datas.isEmpty()) return null
        return PageData(articles = datas, pageCount = articleList.data?.pageCount?.toIntOrNull())
    }
}