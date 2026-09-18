package com.example.fragmject.core.data.repository

import android.util.Log
import com.example.fragmject.core.network.datasource.ArticleDataSource
import com.example.fragmject.core.database.dao.ArticleDao
import com.example.fragmject.core.database.model.toDomain
import com.example.fragmject.core.database.model.toEntity
import com.example.fragmject.core.data.util.fetchAsDomainResult
import com.example.fragmject.core.domain.repository.HomeRepository
import com.example.fragmject.core.domain.result.DomainResult
import com.example.fragmject.core.domain.result.PageData
import com.example.fragmject.core.model.Article
import com.example.fragmject.core.model.ArticleData
import com.example.fragmject.core.model.Banner
import com.example.fragmject.core.network.http.DataResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 只有首页(page 0)进 Room 缓存的 Repository。
 *
 * 数据流：
 * - 页面 0：网络 → Room → Room Flow → UI（秒开）
 * - 页面 1+：网络 → ViewModel 内存列表（不写 Room，消除竞态）
 */
@Singleton
class OfflineFirstArticleRepository @Inject constructor(
    private val articleDao: ArticleDao,
    private val articleRepo: ArticleDataSource,
) : HomeRepository {

    companion object {
        const val CACHE_KEY_BANNER = "home_banners"
        const val CACHE_KEY_TOP = "home_top"
        const val CACHE_KEY_PAGE_0 = "home_page_0"
        private const val TAG = "HomeRepo"
    }

    // ===== 观察 Room（仅页面 0） =====

    /** 观察首页文章（banner + 置顶 + 第 0 页）。 */
    override fun observeHomeArticles(): Flow<List<Article>> = combine(
        observeHomeBanners(),
        observeTopArticles(),
        observePage0(),
    ) { banners, topArticles, page0 ->
        val list = mutableListOf<Article>()
        if (banners.isNotEmpty()) {
            list.add(Article(banners = banners, viewType = 0))
        }
        list.addAll(topArticles)
        list.addAll(page0)
        list
    }

    override fun observeHomeBanners(): Flow<List<Banner>> =
        articleDao.getByCacheKey(CACHE_KEY_BANNER).map { entities ->
            entities.flatMap { entity ->
                val banners = entity.bannersJson
                if (banners.isNotEmpty()) {
                    entity.toDomain().banners ?: emptyList()
                } else emptyList()
            }
        }

    private fun observeTopArticles(): Flow<List<Article>> =
        articleDao.getByCacheKey(CACHE_KEY_TOP).map { entities ->
            entities.map { it.toDomain() }
        }

    /** 只观察第 0 页，不再通过 getByPagePrefix 合并多页。 */
    private fun observePage0(): Flow<List<Article>> =
        articleDao.getByCacheKey(CACHE_KEY_PAGE_0).map { entities ->
            entities.map { it.toDomain() }
        }

    // ===== 网络 → Room 写入 / 直接返回 =====

    /** 刷新首页：写入 Room → Room Flow 自动推送 UI。返回 DomainResult 携带总页数。 */
    override suspend fun refreshHome(): DomainResult<Int> {
        cleanExpiredIfNeeded()

        // banner / top 为尽力而为：失败仅记录日志，不影响主列表。
        runCatching { articleRepo.fetchBannerList() }
            .onSuccess { writeBanners(it) }
            .onFailure { Log.e(TAG, "fetchBannerList failed", it) }
        runCatching { articleRepo.fetchArticleTop() }
            .onSuccess { writeTopArticles(it) }
            .onFailure { Log.e(TAG, "fetchArticleTop failed", it) }

        return fetchAsDomainResult(
            call = { articleRepo.getArticleList(0) },
        ) { resp ->
            writeArticleList(resp)
        }
    }

    /** 加载下一页：仅网络请求，不写 Room。返回文章列表 + 总页数。 */
    override suspend fun loadNextPage(page: Int): DomainResult<PageData> {
        return fetchAsDomainResult(
            call = { articleRepo.getArticleList(page) },
        ) { resp ->
            PageData(
                articles = resp.data?.datas.orEmpty(),
                pageCount = resp.data?.pageCount?.toIntOrNull(),
            )
        }
    }

    // ===== 内部写库方法 =====

    private suspend fun writeBanners(bannerList: DataResponse<List<Banner>>) {
        val banners = bannerList.data ?: return
        if (banners.isEmpty()) return
        val entity = Article(
            id = "0",
            title = "Banners",
            banners = banners,
        ).toEntity(CACHE_KEY_BANNER, 0)
        articleDao.replaceAll(CACHE_KEY_BANNER, listOf(entity))
    }

    private suspend fun writeTopArticles(topArticle: DataResponse<List<Article>>) {
        val articles = topArticle.data ?: return
        if (articles.isEmpty()) return
        articleDao.replaceAll(
            CACHE_KEY_TOP,
            articles.mapIndexed { i, article ->
                article.copy(top = true).toEntity(CACHE_KEY_TOP, i)
            }
        )
    }

    private suspend fun writeArticleList(articleList: DataResponse<ArticleData>): Int {
        val datas = articleList.data?.datas.orEmpty()
        if (datas.isNotEmpty()) {
            articleDao.replaceAll(
                CACHE_KEY_PAGE_0,
                datas.mapIndexed { i, article ->
                    article.toEntity(CACHE_KEY_PAGE_0, i)
                }
            )
        }
        return articleList.data?.pageCount?.toIntOrNull() ?: 0
    }

    // ===== 缓存清理 =====

    /** 每次刷新首页时清理超过 7 天的旧文章缓存，防止 Room 文件无限增长。 */
    private suspend fun cleanExpiredIfNeeded() {
        val threshold = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L
        articleDao.cleanExpired(threshold)
    }
}