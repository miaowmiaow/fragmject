package com.example.fragmject.core.domain.usecase

import com.example.fragmject.core.domain.repository.HomeRepository
import com.example.fragmject.core.domain.result.DomainResult
import com.example.fragmject.core.model.Article
import com.example.fragmject.core.model.Banner
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject

/**
 * 首页 Header 聚合用例。
 *
 * 将「banner 轮播 + 置顶文章」两个数据源的并发拉取从 ViewModel 下沉到领域层，
 * ViewModel 不再自行用 `coroutineScope + async` 编排，只消费聚合结果。
 *
 * [HomeRepository.fetchHomeBanners] / [fetchTopArticles] 现在显式返回 [DomainResult]，
 * 本用例聚合「首个失败」为整体失败，不把失败静默降级为空列表；
 * 降级为空集合的决策保留给 ViewModel 依据产品语义处理。
 */
class HomeHeaderAggregateUseCase @Inject constructor(
    private val homeRepository: HomeRepository,
) {

    /** 首页头部聚合数据。 */
    data class Header(
        val banners: List<Banner>,
        val topArticles: List<Article>,
    )

    /** 并发拉取 banner 与置顶文章并聚合；任一失败返回该失败，否则成功。 */
    suspend operator fun invoke(): DomainResult<Header> = coroutineScope {
        val bannersDeferred = async { homeRepository.fetchHomeBanners() }
        val topDeferred = async { homeRepository.fetchTopArticles() }
        val bannersResult = bannersDeferred.await()
        val topResult = topDeferred.await()
        when {
            bannersResult is DomainResult.Failure -> bannersResult
            topResult is DomainResult.Failure -> topResult
            else -> DomainResult.Success(
                Header(
                    banners = (bannersResult as DomainResult.Success).data,
                    topArticles = (topResult as DomainResult.Success).data,
                )
            )
        }
    }
}
