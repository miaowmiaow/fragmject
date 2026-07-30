package com.example.fragmject.core.data.repository

import com.example.fragmject.core.database.AppDatabase
import com.example.fragmject.core.database.model.toDomain
import com.example.fragmject.core.database.model.toEntity
import com.example.fragmject.core.model.Article
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * 我的收藏 Repository：第 0 页进 Room 缓存，后续页走 ViewModel 内存。
 *
 * getCollectList page 从 0 开始。
 */
class OfflineFirstMyCollectRepository(
    private val articleRepo: ArticleRepository,
) {

    companion object {
        const val CACHE_KEY_PAGE_0 = "my_collect_0"
    }

    /** 只观察第 0 页。 */
    fun observeMyCollect(): Flow<List<Article>> =
        AppDatabase.getArticleDao().getByCacheKey(CACHE_KEY_PAGE_0).map { entities ->
            entities.map { it.toDomain() }
        }

    /** 刷新第 0 页：写 Room → Flow 自动推送。返回 pageCount。 */
    suspend fun refreshMyCollect(): Int? {
        val articleList = runCatching { articleRepo.getCollectList(0) }.getOrNull() ?: return null
        val datas = articleList.data?.datas ?: return null
        AppDatabase.getArticleDao().replaceAll(CACHE_KEY_PAGE_0,
            datas.mapIndexed { i, article -> article.toEntity(CACHE_KEY_PAGE_0, i) })
        return articleList.data?.pageCount?.toIntOrNull()
    }

    /** 加载第 N 页（N>=1）：仅网络，不写 Room。 */
    suspend fun loadMyCollectNextPage(page: Int): PageData? {
        val articleList = runCatching { articleRepo.getCollectList(page) }.getOrNull() ?: return null
        val datas = articleList.data?.datas ?: return null
        if (datas.isEmpty()) return null
        return PageData(articles = datas, pageCount = articleList.data?.pageCount?.toIntOrNull())
    }
}