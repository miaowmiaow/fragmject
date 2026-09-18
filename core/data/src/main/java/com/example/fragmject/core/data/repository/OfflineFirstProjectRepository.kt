package com.example.fragmject.core.data.repository

import com.example.fragmject.core.network.datasource.ProjectDataSource

import com.example.fragmject.core.database.dao.ArticleDao
import com.example.fragmject.core.database.dao.ProjectTreeDao
import com.example.fragmject.core.database.model.toDomain
import com.example.fragmject.core.database.model.toEntity
import com.example.fragmject.core.domain.result.DomainResult
import com.example.fragmject.core.domain.result.PageData
import com.example.fragmject.core.data.util.fetchAsDomainResult
import com.example.fragmject.core.domain.repository.ProjectRepository
import com.example.fragmject.core.model.Article
import com.example.fragmject.core.model.ProjectTree
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 项目 Repository：第 1 页进 Room 缓存，后续页走 ViewModel 内存。
 */
@Singleton
class OfflineFirstProjectRepository @Inject constructor(
    private val projectTreeDao: ProjectTreeDao,
    private val articleDao: ArticleDao,
    private val projectRepo: ProjectDataSource,
) : ProjectRepository {

    companion object {
        const val CACHE_KEY_TREE = "project_tree"
        const val CACHE_KEY_PREFIX = "project"
    }

    // ===== 观察 Room（仅第 1 页） =====

    override fun observeProjectTree(): Flow<List<ProjectTree>> =
        projectTreeDao.getByCacheKey(CACHE_KEY_TREE).map { entities ->
            entities.map { it.toDomain() }
        }

    /** 只观察第 1 页（project_{cid}_1），不再通过 getByPagePrefix 合并多页。 */
    override fun observeProjectArticles(cid: String): Flow<List<Article>> =
        articleDao.getByCacheKey("${CACHE_KEY_PREFIX}_${cid}_1").map { entities ->
            entities.map { it.toDomain() }
        }

    // ===== 网络 → Room 写入 / 直接返回 =====

    override suspend fun refreshProjectTree(): DomainResult<Unit> {
        return fetchAsDomainResult(
            call = { projectRepo.fetchProjectTree() },
        ) { resp ->
            resp.data?.let { data ->
                projectTreeDao.replaceAll(
                    CACHE_KEY_TREE,
                    data.mapIndexed { i, pt -> pt.toEntity(CACHE_KEY_TREE, i) })
            } ?: Unit
        }
    }

    /** 刷新第 1 页：写 Room → Flow 自动推送。 */
    override suspend fun refreshProjectArticles(cid: String): DomainResult<Int> {
        return fetchAsDomainResult(
            call = { projectRepo.getProjectList(1, cid) },
        ) { resp ->
            val datas = resp.data?.datas.orEmpty()
            if (datas.isNotEmpty()) {
                val cacheKey = "${CACHE_KEY_PREFIX}_${cid}_1"
                articleDao.replaceAll(
                    cacheKey,
                    datas.mapIndexed { i, article -> article.toEntity(cacheKey, i) })
            }
            resp.data?.pageCount?.toIntOrNull() ?: 0
        }
    }

    /** 加载第 N 页（N>=2）：仅网络，不写 Room。 */
    override suspend fun loadProjectNextPage(cid: String, page: Int): DomainResult<PageData> {
        return fetchAsDomainResult(
            call = { projectRepo.getProjectList(page, cid) },
        ) { resp ->
            PageData(
                articles = resp.data?.datas.orEmpty(),
                pageCount = resp.data?.pageCount?.toIntOrNull(),
            )
        }
    }
}