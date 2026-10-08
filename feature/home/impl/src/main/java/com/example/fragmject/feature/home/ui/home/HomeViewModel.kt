package com.example.fragmject.feature.home.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import com.example.fragmject.core.domain.CollectState
import com.example.fragmject.core.domain.repository.HomeRepository
import com.example.fragmject.core.domain.result.DomainResult
import com.example.fragmject.core.domain.usecase.HomeHeaderAggregateUseCase
import com.example.fragmject.core.model.Article
import com.example.fragmject.core.model.Banner
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repo: HomeRepository,
    private val headerAggregate: HomeHeaderAggregateUseCase,
    val collectState: CollectState,
) : ViewModel() {

    /** 文章分页数据流（纯网络 PagingSource）。 */
    val pagingFlow = repo.getHomePagingData()
        .cachedIn(viewModelScope)

    private val _banners = MutableStateFlow<List<Banner>>(emptyList())
    val banners: StateFlow<List<Banner>> = _banners.asStateFlow()

    private val _topArticles = MutableStateFlow<List<Article>>(emptyList())
    val topArticles: StateFlow<List<Article>> = _topArticles.asStateFlow()

    private val _headerError = MutableStateFlow<HeaderError?>(null)
    val headerError: StateFlow<HeaderError?> = _headerError.asStateFlow()

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
                    _headerError.value = null
                }
                is DomainResult.Failure -> {
                    // 头部拉取失败时保留旧值，并暴露错误态供 UI 提示/重试
                    _headerError.value = HeaderError(result.code, result.message)
                }
            }
        }
    }

}

/** 首页头部加载错误态。 */
data class HeaderError(
    val code: String,
    val message: String,
)