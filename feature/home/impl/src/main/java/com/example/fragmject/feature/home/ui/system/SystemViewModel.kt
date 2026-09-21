package com.example.fragmject.feature.home.ui.system

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.fragmject.core.domain.repository.NavigationRepository
import com.example.fragmject.core.domain.repository.SystemRepository
import com.example.fragmject.core.domain.usecase.CollectArticleUseCase
import com.example.fragmject.core.model.Article
import com.example.fragmject.core.model.Tree
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

@HiltViewModel
class SystemViewModel @Inject constructor(
    private val repo: SystemRepository,
    private val collectArticle: CollectArticleUseCase,
    private val navigationRepository: NavigationRepository,
) : ViewModel() {

    /** 体系树（与 MainViewModel 共享同一 Room 数据源，供 SystemScreen 定位 cid）。 */
    private val _treeResult = MutableStateFlow<List<Tree>>(emptyList())
    val treeResult: StateFlow<List<Tree>> = _treeResult.asStateFlow()

    /** 每个 cid 的独立分页流缓存，避免 HorizontalPager 切页时重建 Pager 丢失已加载数据。 */
    private val pagerCache = ConcurrentHashMap<String, Flow<PagingData<Article>>>()

    init {
        viewModelScope.launch {
            navigationRepository.observeSystemTree().collect { trees ->
                _treeResult.value = trees
            }
        }
    }

    fun pagingFlow(cid: String): Flow<PagingData<Article>> =
        pagerCache.getOrPut(cid) {
            repo.getSystemPagingData(cid).cachedIn(viewModelScope)
        }

    /** 收藏 / 取消收藏。 */
    suspend fun collect(id: String, collect: Boolean) {
        collectArticle(id, collect)
    }
}