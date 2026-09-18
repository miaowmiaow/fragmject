package com.example.fragmject.core.data.repository

import com.example.fragmject.core.database.dao.HotKeyDao
import com.example.fragmject.core.database.model.toDomain
import com.example.fragmject.core.database.model.toEntity
import com.example.fragmject.core.data.util.fetchAsDomainResult
import com.example.fragmject.core.domain.repository.SearchRepository
import com.example.fragmject.core.domain.result.ArticlePageResult
import com.example.fragmject.core.domain.result.DomainResult
import com.example.fragmject.core.model.HotKey
import com.example.fragmject.core.model.ShareArticle
import com.example.fragmject.core.network.datasource.ArticleDataSource
import com.example.fragmject.core.network.datasource.CommonDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [SearchRepository] 领域端口的 data 层适配器。
 *
 * 热搜词走离线优先（Room 缓存 + 网络刷新），搜索列表直接走远程数据源。
 */
@Singleton
class SearchRepositoryImpl @Inject constructor(
    private val hotKeyDao: HotKeyDao,
    private val articleDataSource: ArticleDataSource,
    private val commonDataSource: CommonDataSource,
) : SearchRepository {

    override fun observeHotKey(): Flow<List<HotKey>> =
        hotKeyDao.getByCacheKey("hotkey").map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun refreshHotKey(): DomainResult<Unit> {
        return fetchAsDomainResult(
            call = { commonDataSource.fetchHotKey() },
        ) { resp ->
            resp.data?.let { data ->
                hotKeyDao.replaceAll(
                    "hotkey",
                    data.mapIndexed { i, hk -> hk.toEntity("hotkey", i) })
            } ?: Unit
        }
    }

    override suspend fun searchArticles(key: String, page: Int): DomainResult<ArticlePageResult> {
        return fetchAsDomainResult(
            call = { articleDataSource.searchArticles(key, page) },
        ) { resp ->
            ArticlePageResult(
                articles = resp.data?.datas.orEmpty(),
                pageCount = resp.data?.pageCount?.toInt(),
            )
        }
    }
}
