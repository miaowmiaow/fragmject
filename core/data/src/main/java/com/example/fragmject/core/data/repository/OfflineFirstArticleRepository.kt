package com.example.fragmject.core.data.repository

import com.example.fragmject.core.database.dao.ArticleDao
import com.example.fragmject.core.database.model.toDomain
import com.example.fragmject.core.database.model.toEntity
import com.example.fragmject.core.model.Article
import com.example.fragmject.core.model.ArticleList
import com.example.fragmject.core.model.Banner
import com.example.fragmject.core.model.BannerList
import com.example.fragmject.core.model.TopArticle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/**
 * 只有首页(page 0)进 Room 缓存的 Repository。
 *
 * 数据流：
 * - 页面 0：网络 → Room → Room Flow → UI（秒开）
 * - 页面 1+：网络 → ViewModel 内存列表（不写 Room，消除竞态）
 */
class OfflineFirstArticleRepository(
    private val articleDao: ArticleDao,
    private val articleRepo: ArticleRepository,
) {

    companion object {
        const val CACHE_KEY_BANNER = "home_banners"
        const val CACHE_KEY_TOP = "home_top"
        const val CACHE_KEY_PAGE_0 = "home_page_0"
    }

    // ===== 观察 Room（仅页面 0） =====

    /** 观察首页文章（banner + 置顶 + 第 0 页）。 */
    fun observeHomeArticles(): Flow<List<Article>> = combine(
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

    fun observeHomeBanners(): Flow<List<Banner>> =
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

    /** 刷新首页：写入 Room → Room Flow 自动推送 UI。返回总页数。 */
    suspend fun refreshHome(): Int? {
        val bannerResult = runCatching { articleRepo.fetchBannerList() }
        val topResult = runCatching { articleRepo.fetchArticleTop() }
        val listResult = runCatching { articleRepo.getArticleList(0) }

        writeBanners(bannerResult.getOrNull())
        writeTopArticles(topResult.getOrNull())
        return writeArticleList(listResult.getOrNull(), page = 0)
    }

    /** 加载下一页：仅网络请求，不写 Room。返回文章列表 + 总页数。 */
    suspend fun loadNextPage(page: Int): PageData? {
        val result = runCatching { articleRepo.getArticleList(page) }
        val articleList = result.getOrNull() ?: return null
        val datas = articleList.data?.datas ?: return null
        if (datas.isEmpty()) return null
        val pageCount = articleList.data?.pageCount?.toIntOrNull()
        return PageData(articles = datas, pageCount = pageCount)
    }

    // ===== 内部写库方法 =====

    private suspend fun writeBanners(bannerList: BannerList?) {
        val banners = bannerList?.data ?: return
        if (banners.isEmpty()) return
        val entity = Article(
            id = "0",
            title = "Banners",
            banners = banners,
        ).toEntity(CACHE_KEY_BANNER, 0)
        articleDao.replaceAll(CACHE_KEY_BANNER, listOf(entity))
    }

    private suspend fun writeTopArticles(topArticle: TopArticle?) {
        val articles = topArticle?.data ?: return
        if (articles.isEmpty()) return
        articleDao.replaceAll(CACHE_KEY_TOP,
            articles.mapIndexed { i, article ->
                article.copy(top = true).toEntity(CACHE_KEY_TOP, i)
            }
        )
    }

    private suspend fun writeArticleList(articleList: ArticleList?, page: Int): Int? {
        val datas = articleList?.data?.datas
        if (datas.isNullOrEmpty()) return null
        val cacheKey = CACHE_KEY_PAGE_0
        articleDao.replaceAll(cacheKey,
            datas.mapIndexed { i, article ->
                article.toEntity(cacheKey, i)
            }
        )
        return articleList.data?.pageCount?.toIntOrNull()
    }
}