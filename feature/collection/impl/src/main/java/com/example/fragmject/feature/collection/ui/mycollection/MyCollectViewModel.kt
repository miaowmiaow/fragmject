package com.example.fragmject.feature.collection.ui.mycollection

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.example.fragmject.core.model.Article
import com.example.fragmject.core.domain.result.DomainResult
import com.example.fragmject.core.domain.usecase.CollectArticleUseCase
import com.example.fragmject.core.domain.usecase.RefreshMyCollectUseCase
import com.example.fragmject.core.domain.usecase.LoadNextMyCollectPageUseCase
import com.example.fragmject.core.domain.usecase.ObserveMyCollectUseCase
import com.example.fragmject.core.common.viewmodel.BaseViewModel
import com.example.fragmject.core.common.viewmodel.PageResult
import com.example.fragmject.core.common.viewmodel.updateSuccessFrom
import com.example.fragmject.core.common.CollectActionHolder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface MyCollectUiState {
    data class Success(
        val isRefreshing: Boolean = false,
        val isLoading: Boolean = false,
        val isFinishing: Boolean = false,
        val result: List<Article> = emptyList(),
    ) : MyCollectUiState
}

val MyCollectUiState.isRefreshing get() = (this as? MyCollectUiState.Success)?.isRefreshing ?: false
val MyCollectUiState.isLoading get() = (this as? MyCollectUiState.Success)?.isLoading ?: false
val MyCollectUiState.isFinishing get() = (this as? MyCollectUiState.Success)?.isFinishing ?: false
val MyCollectUiState.result get() = (this as? MyCollectUiState.Success)?.result ?: emptyList()

private const val TAG = "MyCollectVM"

@HiltViewModel
class MyCollectViewModel @Inject constructor(
    private val observeMyCollect: ObserveMyCollectUseCase,
    private val refreshMyCollect: RefreshMyCollectUseCase,
    private val loadNextMyCollectPage: LoadNextMyCollectPageUseCase,
    private val collectArticle: CollectArticleUseCase,
) : BaseViewModel(), CollectActionHolder {

    private val _uiState = MutableStateFlow<MyCollectUiState>(MyCollectUiState.Success())
    val uiState: StateFlow<MyCollectUiState> = _uiState.asStateFlow()

    /** 页面 1+ 的内存列表。 */
    private val extraPages = mutableListOf<Article>()

    init {
        // 观察 Room 中第 0 页
        viewModelScope.launch {
            observeMyCollect().collect { page0 ->
                _uiState.updateSuccessFrom({ MyCollectUiState.Success() }) {
                    it.copy(isRefreshing = false, result = page0 + extraPages)
                }
            }
        }
        getHome()
    }

    fun getHome(userTriggered: Boolean = false) {
        extraPages.clear()

        viewModelScope.launch {
            getHomePage(0)
            _uiState.updateSuccessFrom({ MyCollectUiState.Success() }) { it.copy(isRefreshing = userTriggered) }
            val pageCount = when (val r = refreshMyCollect()) {
                is DomainResult.Success -> r.data
                is DomainResult.Failure -> {
                    Log.e(TAG, "refreshMyCollect failed: ${r.code} ${r.message}")
                    null
                }
            }
            val paging = finishPage(pageCount)
            _uiState.updateSuccessFrom({ MyCollectUiState.Success() }) {
                it.copy(isRefreshing = false, isLoading = paging.isLoading, isFinishing = paging.isFinishing)
            }
        }
    }

    fun getNext() {
        _uiState.updateSuccessFrom({ MyCollectUiState.Success() }) { it.copy(isLoading = false) }
        viewModelScope.launch {
            val result = loadNextPage { page ->
                when (val r = loadNextMyCollectPage(page)) {
                    is DomainResult.Success -> {
                        if (r.data.articles.isEmpty()) {
                            PageResult(success = false)
                        } else {
                            PageResult(items = r.data.articles, pageCount = r.data.pageCount)
                        }
                    }
                    is DomainResult.Failure -> {
                        Log.e(TAG, "loadNextMyCollectPage failed: ${r.code} ${r.message}")
                        PageResult(success = false)
                    }
                }
            }
            if (!result.success) {
                _uiState.updateSuccessFrom({ MyCollectUiState.Success() }) { it.copy(isLoading = false, isFinishing = true) }
                return@launch
            }
            extraPages.addAll(result.items)
            _uiState.updateSuccessFrom({ MyCollectUiState.Success() }) {
                it.copy(
                    result = it.result + result.items,
                    isLoading = result.hasMore,
                    isFinishing = !result.hasMore,
                )
            }
        }
    }

    /** 收藏 / 取消收藏。 */
    override val collectAction: suspend (String, Boolean) -> Unit = { id, collect ->
        collectArticle(id, collect)
    }
}