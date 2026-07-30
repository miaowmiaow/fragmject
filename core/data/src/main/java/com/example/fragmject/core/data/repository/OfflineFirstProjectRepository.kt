package com.example.fragmject.core.data.repository

import com.example.fragmject.core.database.AppDatabase
import com.example.fragmject.core.database.model.toDomain
import com.example.fragmject.core.database.model.toEntity
import com.example.fragmject.core.model.Article
import com.example.fragmject.core.model.ProjectTree
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * 项目 Repository：第 1 页进 Room 缓存，后续页走 ViewModel 内存。
 */
class OfflineFirstProjectRepository(
    private val projectRepo: ProjectRepository,
) {

    companion object {
        const val CACHE_KEY_TREE = "project_tree"
        const val CACHE_KEY_PREFIX = "project"
    }

    // ===== 观察 Room（仅第 1 页） =====

    fun observeProjectTree(): Flow<List<ProjectTree>> =
        AppDatabase.getProjectTreeDao().getByCacheKey(CACHE_KEY_TREE).map { entities ->
            entities.map { it.toDomain() }
        }

    /** 只观察第 1 页（project_{cid}_1），不再通过 getByPagePrefix 合并多页。 */
    fun observeProjectArticles(cid: String): Flow<List<Article>> =
        AppDatabase.getArticleDao().getByCacheKey("${CACHE_KEY_PREFIX}_${cid}_1").map { entities ->
            entities.map { it.toDomain() }
        }

    // ===== 网络 → Room 写入 / 直接返回 =====

    suspend fun refreshProjectTree() {
        val result = runCatching { projectRepo.fetchProjectTree() }
        val data = result.getOrNull()?.data ?: return
        AppDatabase.getProjectTreeDao().replaceAll(CACHE_KEY_TREE,
            data.mapIndexed { i, pt -> pt.toEntity(CACHE_KEY_TREE, i) })
    }

    /** 刷新第 1 页：写 Room → Flow 自动推送。 */
    suspend fun refreshProjectArticles(cid: String): Int? {
        val articleList = runCatching { projectRepo.getProjectList(cid, 1) }.getOrNull() ?: return null
        val datas = articleList.data?.datas ?: return null
        val cacheKey = "${CACHE_KEY_PREFIX}_${cid}_1"
        AppDatabase.getArticleDao().replaceAll(cacheKey,
            datas.mapIndexed { i, article -> article.toEntity(cacheKey, i) })
        return articleList.data?.pageCount?.toIntOrNull()
    }

    /** 加载第 N 页（N>=2）：仅网络，不写 Room。 */
    suspend fun loadProjectNextPage(cid: String, page: Int): PageData? {
        val articleList = runCatching { projectRepo.getProjectList(cid, page) }.getOrNull() ?: return null
        val datas = articleList.data?.datas ?: return null
        if (datas.isEmpty()) return null
        return PageData(articles = datas, pageCount = articleList.data?.pageCount?.toIntOrNull())
    }
}