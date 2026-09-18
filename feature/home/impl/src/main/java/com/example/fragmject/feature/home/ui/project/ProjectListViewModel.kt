package com.example.fragmject.feature.home.ui.project

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.example.fragmject.core.model.Article
import com.example.fragmject.core.domain.result.DomainResult
import com.example.fragmject.core.domain.usecase.CollectArticleUseCase
import com.example.fragmject.core.domain.usecase.LoadNextProjectPageUseCase
import com.example.fragmject.core.domain.usecase.ObserveProjectArticlesUseCase
import com.example.fragmject.core.domain.usecase.RefreshProjectArticlesUseCase
import com.example.fragmject.core.common.viewmodel.BaseViewModel
import com.example.fragmject.core.common.viewmodel.PageResult
import com.example.fragmject.core.common.viewmodel.updateSuccessFrom
import com.example.fragmject.core.common.CollectActionHolder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

sealed interface ProjectListUiState {
    data class Success(
        val isRefreshing: Map<String, Boolean> = emptyMap(),
        val isLoading: Map<String, Boolean> = emptyMap(),
        val isFinishing: Map<String, Boolean> = emptyMap(),
        val result: Map<String, List<Article>> = emptyMap(),
    ) : ProjectListUiState

    fun getRefreshing(cid: String): Boolean = (this as? Success)?.isRefreshing?.get(cid) ?: false
    fun getLoading(cid: String): Boolean = (this as? Success)?.isLoading?.get(cid) ?: false
    fun getFinishing(cid: String): Boolean = (this as? Success)?.isFinishing?.get(cid) ?: false
    fun getResult(cid: String): List<Article>? = (this as? Success)?.result?.get(cid)
}

val ProjectListUiState.isRefreshing get() = (this as? ProjectListUiState.Success)?.isRefreshing ?: emptyMap()
val ProjectListUiState.isLoading get() = (this as? ProjectListUiState.Success)?.isLoading ?: emptyMap()
val ProjectListUiState.isFinishing get() = (this as? ProjectListUiState.Success)?.isFinishing ?: emptyMap()
val ProjectListUiState.result get() = (this as? ProjectListUiState.Success)?.result ?: emptyMap()

private const val TAG = "ProjectListVM"

@HiltViewModel
class ProjectListViewModel @Inject constructor(
    private val observeProjectArticles: ObserveProjectArticlesUseCase,
    private val refreshProjectArticles: RefreshProjectArticlesUseCase,
    private val loadNextProjectPage: LoadNextProjectPageUseCase,
    private val collectArticle: CollectArticleUseCase,
) : BaseViewModel(), CollectActionHolder {

    private val _uiState = MutableStateFlow<ProjectListUiState>(ProjectListUiState.Success())
    val uiState: StateFlow<ProjectListUiState> = _uiState.asStateFlow()

    /** 每个 cid 的页面 2+ 内存列表。 */
    private val extraPages = ConcurrentHashMap<String, MutableList<Article>>()

    fun init(cid: String) {
        if (!uiState.value.result.containsKey(cid)) {
            // 观察 Room 中第 1 页（project_{cid}_1）
            viewModelScope.launch {
                observeProjectArticles(cid).collect { page1 ->
                    _uiState.updateSuccessFrom({ ProjectListUiState.Success() }) { state ->
                        val extras = extraPages[cid].orEmpty()
                        state.copy(
                            isRefreshing = state.isRefreshing + (cid to false),
                            result = state.result + (cid to (page1 + extras)),
                        )
                    }
                }
            }
            getHome(cid)
        }
    }

    fun getHome(cid: String, userTriggered: Boolean = false) {
        extraPages.remove(cid)

        _uiState.updateSuccessFrom({ ProjectListUiState.Success() }) { state ->
            state.copy(isRefreshing = state.isRefreshing + (cid to userTriggered))
        }
        viewModelScope.launch {
            getHomePage(1, cid)
            val pageCount = when (val r = refreshProjectArticles(cid)) {
                is DomainResult.Success -> r.data
                is DomainResult.Failure -> {
                    Log.e(TAG, "refreshProjectArticles failed cid=$cid: ${r.code} ${r.message}")
                    null
                }
            }
            val paging = finishPage(pageCount, cid)
            _uiState.updateSuccessFrom({ ProjectListUiState.Success() }) { state ->
                state.copy(
                    isRefreshing = state.isRefreshing + (cid to false),
                    isLoading = state.isLoading + (cid to paging.isLoading),
                    isFinishing = state.isFinishing + (cid to paging.isFinishing),
                )
            }
        }
    }

    fun getNext(cid: String) {
        _uiState.updateSuccessFrom({ ProjectListUiState.Success() }) { state ->
            state.copy(isLoading = state.isLoading + (cid to false))
        }
        viewModelScope.launch {
            val result = loadNextPage(key = cid) { page ->
                when (val r = loadNextProjectPage(cid, page)) {
                    is DomainResult.Success -> {
                        if (r.data.articles.isEmpty()) {
                            PageResult(success = false)
                        } else {
                            PageResult(items = r.data.articles, pageCount = r.data.pageCount)
                        }
                    }
                    is DomainResult.Failure -> {
                        Log.e(TAG, "loadNextProjectPage failed cid=$cid: ${r.code} ${r.message}")
                        PageResult(success = false)
                    }
                }
            }
            if (!result.success) {
                _uiState.updateSuccessFrom({ ProjectListUiState.Success() }) { state ->
                    state.copy(
                        isLoading = state.isLoading + (cid to false),
                        isFinishing = state.isFinishing + (cid to true),
                    )
                }
                return@launch
            }
            extraPages.getOrPut(cid) { mutableListOf() }.addAll(result.items)
            _uiState.updateSuccessFrom({ ProjectListUiState.Success() }) { state ->
                state.copy(
                    result = state.result + (cid to (state.result[cid].orEmpty() + result.items)),
                    isLoading = state.isLoading + (cid to result.hasMore),
                    isFinishing = state.isFinishing + (cid to !result.hasMore),
                )
            }
        }
    }

    /** 收藏 / 取消收藏。 */
    override val collectAction: suspend (String, Boolean) -> Unit = { id, collect ->
        collectArticle(id, collect)
    }
}