package com.example.fragmject.feature.wan.my_collect

import androidx.lifecycle.viewModelScope
import com.example.fragmject.core.model.Article
import com.example.fragmject.core.data.repository.MyCollectRepository
import com.example.fragmject.core.domain.usecase.RefreshMyCollectUseCase
import com.example.fragmject.core.domain.usecase.LoadNextMyCollectPageUseCase
import com.example.fragmject.core.common.TransitionGuard
import com.example.fragmject.core.common.viewmodel.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

private inline fun MutableStateFlow<MyCollectUiState>.updateData(
    crossinline block: (MyCollectUiState.Success) -> MyCollectUiState.Success,
) {
    update { if (it is MyCollectUiState.Success) block(it) else it }
}

@HiltViewModel
class MyCollectViewModel @Inject constructor(
    private val repo: MyCollectRepository,
    private val refreshMyCollect: RefreshMyCollectUseCase,
    private val loadNextMyCollectPage: LoadNextMyCollectPageUseCase,
) : BaseViewModel() {

    private val _uiState = MutableStateFlow<MyCollectUiState>(MyCollectUiState.Success())
    val uiState: StateFlow<MyCollectUiState> = _uiState.asStateFlow()

    /** 页面 1+ 的内存列表。 */
    private val extraPages = mutableListOf<Article>()

    init {
        // 观察 Room 中第 0 页
        viewModelScope.launch {
            repo.observeMyCollect().collect { page0 ->
                withContext(Dispatchers.Default) { page0.onEach { it.preloadForDisplay() } }
                _uiState.updateData {
                    it.copy(isRefreshing = false, result = page0 + extraPages)
                }
            }
        }
        getHome()
    }

    fun getHome() {
        extraPages.clear()

        viewModelScope.launch {
            getHomePage(0)
            _uiState.updateData { it.copy(isRefreshing = true) }
            val t = TransitionGuard.now()
            val pageCount = refreshMyCollect()
            TransitionGuard.await(t)
            updatePageCont(pageCount)
            val hasNext = hasNextPage()
            _uiState.updateData {
                it.copy(isRefreshing = false, isLoading = hasNext, isFinishing = !hasNext)
            }
        }
    }

    fun getNext() {
        _uiState.updateData { it.copy(isLoading = false) }
        viewModelScope.launch {
            val page = getNextPage()
            val result = loadNextMyCollectPage(page)
            if (!result.hasMore && result.articles.isEmpty()) {
                updatePageCont(page - 1)
                _uiState.updateData { it.copy(isLoading = false, isFinishing = true) }
                return@launch
            }
            updatePageCont(result.pageCount)
            val hasNext = hasNextPage()
            withContext(Dispatchers.Default) { result.articles.onEach { it.preloadForDisplay() } }
            extraPages.addAll(result.articles)
            _uiState.updateData {
                it.copy(
                    result = it.result + result.articles,
                    isLoading = hasNext,
                    isFinishing = !hasNext,
                )
            }
        }
    }
}