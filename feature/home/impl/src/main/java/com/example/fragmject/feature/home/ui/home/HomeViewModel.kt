package com.example.fragmject.feature.home.ui.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import com.example.fragmject.core.domain.repository.HomeRepository
import com.example.fragmject.core.domain.result.DomainResult
import com.example.fragmject.core.domain.usecase.CollectArticleUseCase
import com.example.fragmject.core.domain.usecase.HomeHeaderAggregateUseCase
import com.example.fragmject.core.model.Article
import com.example.fragmject.core.model.Banner
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "HomeVM"

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repo: HomeRepository,
    private val headerAggregate: HomeHeaderAggregateUseCase,
    private val collectArticle: CollectArticleUseCase,
) : ViewModel() {

    /** 文章分页数据流（纯网络 PagingSource）。 */
    val pagingFlow = repo.getHomePagingData()
        .cachedIn(viewModelScope)

    private val _banners = MutableStateFlow<List<Banner>>(emptyList())
    val banners: StateFlow<List<Banner>> = _banners.asStateFlow()

    private val _topArticles = MutableStateFlow<List<Article>>(emptyList())
    val topArticles: StateFlow<List<Article>> = _topArticles.asStateFlow()

    init {
        loadHeader()
    }

    /** 拉取首页头部（banner + 置顶文章），编排已下沉到 [HomeHeaderAggregateUseCase]。 */
    fun loadHeader() {
        viewModelScope.launch {
            when (val result = headerAggregate()) {
                is DomainResult.Success -> {
                    _banners.value = result.data.banners
                    _topArticles.value = result.data.topArticles
                }
                is DomainResult.Failure -> {
                    // 头部拉取失败时保留旧值，仅记录日志；是否展示错误态由产品语义决定。
                    Log.e(TAG, "load header failed: ${result.code} ${result.message}")
                }
            }
        }
    }

    /** 收藏 / 取消收藏，供 FeedCard 的 onToggleClick 直接绑定。 */
    suspend fun collect(id: String, collect: Boolean) {
        collectArticle(id, collect)
    }
}