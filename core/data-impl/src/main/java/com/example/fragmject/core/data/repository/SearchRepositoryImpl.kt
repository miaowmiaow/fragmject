package com.example.fragmject.core.data.impl.repository

import androidx.paging.Pager
import androidx.paging.PagingData
import com.example.fragmject.core.data.impl.paging.DEFAULT_PAGING_CONFIG
import com.example.fragmject.core.data.contract.local.HotKeyLocalDataSource
import com.example.fragmject.core.data.impl.paging.SearchPagingSource
import com.example.fragmject.core.data.impl.util.fetchAsDomainResult
import com.example.fragmject.core.domain.repository.SearchRepository
import com.example.fragmject.core.domain.result.DomainResult
import com.example.fragmject.core.model.Article
import com.example.fragmject.core.model.HotKey
import com.example.fragmject.core.data.contract.remote.ArticleRemoteDataSource
import com.example.fragmject.core.data.contract.remote.CommonRemoteDataSource
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [SearchRepository] 领域端口的 data 层适配器。
 *
 * 热搜词走离线优先（Room 缓存 + 网络刷新），搜索列表直接走远程数据源。
 */
@Singleton
class SearchRepositoryImpl @Inject constructor(
    private val hotKeyLocal: HotKeyLocalDataSource,
    private val articleDataSource: ArticleRemoteDataSource,
    private val commonDataSource: CommonRemoteDataSource,
) : SearchRepository {

    override fun observeHotKey(): Flow<List<HotKey>> = hotKeyLocal.observeHotKey()

    override suspend fun refreshHotKey(): DomainResult<Unit> {
        return fetchAsDomainResult(
            call = { commonDataSource.fetchHotKey() },
        ) { resp ->
            resp.data?.let { data ->
                hotKeyLocal.saveHotKey(data)
            } ?: Unit
        }
    }

    override fun getSearchPagingData(key: String): Flow<PagingData<Article>> = Pager(
        config = DEFAULT_PAGING_CONFIG,
        pagingSourceFactory = { SearchPagingSource(key, articleDataSource) },
    ).flow
}
