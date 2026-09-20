package com.example.fragmject.feature.home.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import com.example.fragmject.core.ui.contract.CollectActionHolder
import com.example.fragmject.core.domain.repository.HomeRepository
import com.example.fragmject.core.domain.usecase.CollectArticleUseCase
import com.example.fragmject.core.model.Article
import com.example.fragmject.core.model.Banner
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repo: HomeRepository,
    private val collectArticle: CollectArticleUseCase,
) : ViewModel(), CollectActionHolder {

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

    /** 网络拉取 banner 与置顶文章，失败静默降级为空列表。 */
    fun loadHeader() {
        viewModelScope.launch {
            coroutineScope {
                val bannersDeferred = async { repo.fetchHomeBanners() }
                val topDeferred = async { repo.fetchTopArticles() }
                _banners.value = bannersDeferred.await()
                _topArticles.value = topDeferred.await()
            }
        }
    }

    /** 收藏 / 取消收藏，供 ArticleCard 的 onCollectClick 直接绑定。 */
    override val collectAction: suspend (String, Boolean) -> Unit = { id, collect ->
        collectArticle(id, collect)
    }
}