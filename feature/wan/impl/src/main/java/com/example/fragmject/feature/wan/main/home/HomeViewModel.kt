package com.example.fragmject.feature.wan.main.home

import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.viewModelScope
import com.example.fragmject.core.common.TransitionGuard
import com.example.fragmject.core.common.viewmodel.BaseViewModel
import com.example.fragmject.core.data.repository.HomeRepository
import com.example.fragmject.core.domain.usecase.LoadNextHomePageUseCase
import com.example.fragmject.core.domain.usecase.RefreshHomeUseCase
import com.example.fragmject.core.model.Article
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Success(
        val isRefreshing: Boolean = false,
        val isLoading: Boolean = false,
        val isFinishing: Boolean = false,
        val result: List<Article> = emptyList(),
    ) : HomeUiState
}

val HomeUiState.isRefreshing get() = (this as? HomeUiState.Success)?.isRefreshing ?: false
val HomeUiState.isLoading get() = (this as? HomeUiState.Success)?.isLoading ?: false
val HomeUiState.isFinishing get() = (this as? HomeUiState.Success)?.isFinishing ?: false
val HomeUiState.result get() = (this as? HomeUiState.Success)?.result ?: emptyList()

private const val TAG = "HomeVM"

private inline fun MutableStateFlow<HomeUiState>.updateData(
    crossinline block: (HomeUiState.Success) -> HomeUiState.Success,
) {
    update {
        val next = if (it is HomeUiState.Success) block(it) else it
        Log.d(
            TAG,
            "updateData -> isRefreshing=${next.isRefreshing} isLoading=${next.isLoading} isFinishing=${next.isFinishing} result.size=${next.result.size}"
        )
        next
    }
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val homeRepo: HomeRepository,
    private val refreshHome: RefreshHomeUseCase,
    private val loadNextHomePage: LoadNextHomePageUseCase,
) : BaseViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Success())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    /** Compose 可观测的文章列表。原地增删 — 不创建新 List 实例，避免
     *  Strong Skipping 下因引用变更导致的全量 skip 失败。 */
    private val resultList = mutableStateListOf<Article>()

    /** 当前 Room 返回的文章数量，用于在刷新时区分 Room 页面与内存额外页。 */
    private var roomArticleCount = 0

    init {
        // 观察 Room（仅 banner + top + page 0）
        viewModelScope.launch {
            homeRepo.observeHomeArticles().collect { roomArticles ->
                withContext(Dispatchers.Default) { roomArticles.onEach { it.preloadForDisplay() } }
                roomArticleCount = roomArticles.size
                val extras = resultList.drop(roomArticleCount).toList()
                resultList.clear()
                resultList.addAll(roomArticles)
                resultList.addAll(extras)
                _uiState.updateData {
                    it.copy(result = resultList, isRefreshing = false)
                }
            }
        }
        // 首次加载
        getHomePage(0)
        viewModelScope.launch {
            val t = TransitionGuard.now()
            val pageCount = refreshHome()
            TransitionGuard.await(t)
            onPageLoaded(pageCount)
        }
    }

    fun getHome(userTriggered: Boolean = false) {
        // 移除额外页，仅保留 Room 文章
        while (resultList.size > roomArticleCount) {
            resultList.removeAt(resultList.lastIndex)
        }
        _uiState.updateData { it.copy(isRefreshing = userTriggered) }
        getHomePage(0)
        viewModelScope.launch {
            val t = TransitionGuard.now()
            val pageCount = refreshHome()
            TransitionGuard.await(t)
            onPageLoaded(pageCount)
        }
    }

    fun getNext() {
        _uiState.updateData { it.copy(isLoading = false) }
        viewModelScope.launch {
            val page = getNextPage()
            val result = loadNextHomePage(page)
            if (!result.hasMore && result.articles.isEmpty()) {
                // 失败或无更多数据
                updatePageCont(page - 1)
                _uiState.updateData { it.copy(isLoading = false, isFinishing = true) }
                return@launch
            }
            updatePageCont(result.pageCount)
            // 原地追加：不创建新 List，Compose 自动追踪增量变更
            withContext(Dispatchers.Default) { result.articles.onEach { it.preloadForDisplay() } }
            resultList.addAll(result.articles)
            _uiState.updateData {
                it.copy(
                    result = resultList,
                    isLoading = result.hasMore,
                    isFinishing = !result.hasMore,
                )
            }
        }
    }

    private fun onPageLoaded(pageCount: Int?) {
        updatePageCont(pageCount)
        val hasNext = hasNextPage()
        _uiState.updateData {
            it.copy(
                isRefreshing = false,
                isLoading = hasNext,
                isFinishing = !hasNext,
            )
        }
    }
}