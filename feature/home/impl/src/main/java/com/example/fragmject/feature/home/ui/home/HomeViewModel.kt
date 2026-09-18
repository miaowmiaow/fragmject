package com.example.fragmject.feature.home.ui.home

import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.viewModelScope
import com.example.fragmject.core.common.viewmodel.BaseViewModel
import com.example.fragmject.core.common.viewmodel.PageResult
import com.example.fragmject.core.common.viewmodel.updateSuccessFrom
import com.example.fragmject.core.common.debug.DebugBridge
import com.example.fragmject.core.domain.result.DomainResult
import com.example.fragmject.core.domain.usecase.CollectArticleUseCase
import com.example.fragmject.core.domain.usecase.LoadNextHomePageUseCase
import com.example.fragmject.core.domain.usecase.ObserveHomeArticlesUseCase
import com.example.fragmject.core.domain.usecase.RefreshHomeUseCase
import com.example.fragmject.core.model.Article
import com.example.fragmject.core.common.CollectActionHolder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Success(
        val isRefreshing: Boolean = false,
        val isLoading: Boolean = false,
        val isFinishing: Boolean = false,
        // 契约约定：该字段实际存的是 mutableStateListOf<Article>（SnapshotStateList），
        // 而非普通 List。依赖 Strong Skipping + Snapshot 系统原地增删触发重组；
        // 切勿将其转为 toList() 或替换为普通 List，否则会破坏原地更新契约。
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
    updateSuccessFrom({ HomeUiState.Success() }) { success ->
        val next = block(success)
        if (DebugBridge.isDebugBuild) {
            Log.d(
                TAG,
                "updateData -> isRefreshing=${next.isRefreshing} isLoading=${next.isLoading} isFinishing=${next.isFinishing} result.size=${next.result.size}"
            )
        }
        next
    }
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val observeHomeArticles: ObserveHomeArticlesUseCase,
    private val refreshHome: RefreshHomeUseCase,
    private val loadNextHomePage: LoadNextHomePageUseCase,
    private val collectArticle: CollectArticleUseCase,
) : BaseViewModel(), CollectActionHolder {

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
            observeHomeArticles().collect { roomArticles ->
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
            onPageLoaded(refreshHome())
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
            onPageLoaded(refreshHome())
        }
    }

    fun getNext() {
        _uiState.updateData { it.copy(isLoading = false) }
        viewModelScope.launch {
            val result = loadNextPage { page ->
                when (val r = loadNextHomePage(page)) {
                    is DomainResult.Success -> {
                        if (r.data.articles.isEmpty()) {
                            PageResult(success = false)
                        } else {
                            PageResult(items = r.data.articles, pageCount = r.data.pageCount)
                        }
                    }
                    is DomainResult.Failure -> {
                        Log.e(TAG, "loadNextHomePage failed: ${r.code} ${r.message}")
                        PageResult(success = false)
                    }
                }
            }
            if (!result.success) {
                _uiState.updateData { it.copy(isLoading = false, isFinishing = true) }
                return@launch
            }
            // 原地追加：不创建新 List，Compose 自动追踪增量变更
            resultList.addAll(result.items)
            _uiState.updateData {
                it.copy(
                    result = resultList,
                    isLoading = result.hasMore,
                    isFinishing = !result.hasMore,
                )
            }
        }
    }

    private fun onPageLoaded(result: DomainResult<Int>) {
        val pageCount = when (result) {
            is DomainResult.Success -> result.data
            is DomainResult.Failure -> {
                Log.e(TAG, "refreshHome failed: ${result.code} ${result.message}")
                null
            }
        }
        val paging = finishPage(pageCount)
        _uiState.updateData {
            it.copy(
                isRefreshing = false,
                isLoading = paging.isLoading,
                isFinishing = paging.isFinishing,
            )
        }
    }

    /** 收藏 / 取消收藏，供 ArticleCard 的 onCollectClick 直接绑定。 */
    override val collectAction: suspend (String, Boolean) -> Unit = { id, collect ->
        collectArticle(id, collect)
    }
}