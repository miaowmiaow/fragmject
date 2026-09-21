package com.example.fragmject.feature.home.ui.project

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.fragmject.core.domain.repository.ProjectRepository
import com.example.fragmject.core.domain.usecase.CollectArticleUseCase
import com.example.fragmject.core.model.Article
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

@HiltViewModel
class ProjectListViewModel @Inject constructor(
    private val repo: ProjectRepository,
    private val collectArticle: CollectArticleUseCase,
) : ViewModel() {

    /** 每个 cid 的独立分页流缓存，避免 HorizontalPager 切页时重建 Pager 丢失已加载数据。 */
    private val pagerCache = ConcurrentHashMap<String, Flow<PagingData<Article>>>()

    fun pagingFlow(cid: String): Flow<PagingData<Article>> =
        pagerCache.getOrPut(cid) {
            repo.getProjectPagingData(cid).cachedIn(viewModelScope)
        }

    /** 收藏 / 取消收藏。 */
    suspend fun collect(id: String, collect: Boolean) {
        collectArticle(id, collect)
    }
}