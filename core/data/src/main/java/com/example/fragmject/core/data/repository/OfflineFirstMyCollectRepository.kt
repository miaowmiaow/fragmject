package com.example.fragmject.core.data.repository

import com.example.fragmject.core.network.datasource.ArticleDataSource

import com.example.fragmject.core.database.dao.ArticleDao
import com.example.fragmject.core.database.model.toDomain
import com.example.fragmject.core.database.model.toEntity
import com.example.fragmject.core.data.util.fetchAsDomainResult
import com.example.fragmject.core.domain.repository.MyCollectRepository
import com.example.fragmject.core.domain.result.DomainResult
import com.example.fragmject.core.domain.result.PageData
import com.example.fragmject.core.model.Article
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 我的收藏 Repository：第 0 页进 Room 缓存，后续页走 ViewModel 内存。
 *
 * getCollectList page 从 0 开始。
 */
@Singleton
class OfflineFirstMyCollectRepository @Inject constructor(
    private val articleDao: ArticleDao,
    private val articleRepo: ArticleDataSource,
) : MyCollectRepository {

    companion object {
        const val CACHE_KEY_PAGE_0 = "my_collect_0"
    }

    /** 只观察第 0 页。 */
    override fun observeMyCollect(): Flow<List<Article>> =
        articleDao.getByCacheKey(CACHE_KEY_PAGE_0).map { entities ->
            entities.map { it.toDomain() }
        }

    /** 刷新第 0 页：写 Room → Flow 自动推送。 */
    override suspend fun refreshMyCollect(): DomainResult<Int> {
        return fetchAsDomainResult(
            call = { articleRepo.getCollectList(0) },
        ) { resp ->
            val datas = resp.data?.datas.orEmpty()
            if (datas.isNotEmpty()) {
                articleDao.replaceAll(
                    CACHE_KEY_PAGE_0,
                    datas.mapIndexed { i, article ->
                        article.toEntity(
                            CACHE_KEY_PAGE_0,
                            i
                        )
                    })
            }
            resp.data?.pageCount?.toIntOrNull() ?: 0
        }
    }

    /** 加载第 N 页（N>=1）：仅网络，不写 Room。 */
    override suspend fun loadMyCollectNextPage(page: Int): DomainResult<PageData> {
        return fetchAsDomainResult(
            call = { articleRepo.getCollectList(page) },
        ) { resp ->
            PageData(
                articles = resp.data?.datas.orEmpty(),
                pageCount = resp.data?.pageCount?.toIntOrNull(),
            )
        }
    }
}