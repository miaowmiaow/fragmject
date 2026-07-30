package com.example.fragmject.feature.wan.system

import androidx.lifecycle.viewModelScope
import com.example.fragmject.core.model.Article
import com.example.fragmject.core.data.repository.SystemRepository
import com.example.fragmject.core.domain.usecase.RefreshSystemArticlesUseCase
import com.example.fragmject.core.domain.usecase.LoadNextSystemPageUseCase
import com.example.fragmject.core.common.TransitionGuard
import com.example.fragmject.core.common.viewmodel.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

sealed interface SystemUiState {
    data class Success(
        val isRefreshing: Map<String, Boolean> = emptyMap(),
        val isLoading: Map<String, Boolean> = emptyMap(),
        val isFinishing: Map<String, Boolean> = emptyMap(),
        val result: Map<String, List<Article>> = emptyMap(),
    ) : SystemUiState

    fun getRefreshing(cid: String): Boolean = isRefreshing[cid] ?: false
    fun getLoading(cid: String): Boolean = isLoading[cid] ?: false
    fun getFinishing(cid: String): Boolean = isFinishing[cid] ?: false
    fun getResult(cid: String): List<Article>? = result[cid]
}

val SystemUiState.isRefreshing get() = (this as? SystemUiState.Success)?.isRefreshing ?: emptyMap()
val SystemUiState.isLoading get() = (this as? SystemUiState.Success)?.isLoading ?: emptyMap()
val SystemUiState.isFinishing get() = (this as? SystemUiState.Success)?.isFinishing ?: emptyMap()
val SystemUiState.result get() = (this as? SystemUiState.Success)?.result ?: emptyMap()

private inline fun MutableStateFlow<SystemUiState>.updateData(
    crossinline block: (SystemUiState.Success) -> SystemUiState.Success,
) {
    update {
        val current = (it as? SystemUiState.Success) ?: SystemUiState.Success()
        block(current)
    }
}

@HiltViewModel
class SystemViewModel @Inject constructor(
    private val repo: SystemRepository,
    private val refreshSystemArticles: RefreshSystemArticlesUseCase,
    private val loadNextSystemPage: LoadNextSystemPageUseCase,
) : BaseViewModel() {

    private val _uiState = MutableStateFlow<SystemUiState>(SystemUiState.Success())
    val uiState: StateFlow<SystemUiState> = _uiState.asStateFlow()

    /** 每个 cid 的页面 1+ 内存列表。 */
    private val extraPages = ConcurrentHashMap<String, MutableList<Article>>()

    fun init(cid: String) {
        if (!uiState.value.result.containsKey(cid)) {
            // 观察 Room 中第 0 页（system_{cid}_0）
            viewModelScope.launch {
                repo.observeSystemArticles(cid).collect { page0 ->
                    page0.onEach { it.preloadForDisplay() }
                    _uiState.updateData { state ->
                        val extras = extraPages[cid].orEmpty()
                        state.copy(
                            isRefreshing = state.isRefreshing + (cid to false),
                            result = state.result + (cid to (page0 + extras)),
                        )
                    }
                }
            }
            getHome(cid)
        }
    }

    fun getHome(cid: String) {
        extraPages.remove(cid)

        viewModelScope.launch {
            getHomePage(0, cid)
            _uiState.updateData { state ->
                state.copy(isRefreshing = state.isRefreshing + (cid to true))
            }
            val t = TransitionGuard.now()
            val pageCount = refreshSystemArticles(cid)
            TransitionGuard.await(t)
            updatePageCont(pageCount, cid)
            val hasNext = hasNextPage(cid)
            _uiState.updateData { state ->
                state.copy(
                    isRefreshing = state.isRefreshing + (cid to false),
                    isLoading = state.isLoading + (cid to hasNext),
                    isFinishing = state.isFinishing + (cid to !hasNext),
                )
            }
        }
    }

    fun getNext(cid: String) {
        _uiState.updateData { state ->
            state.copy(isLoading = state.isLoading + (cid to false))
        }
        viewModelScope.launch {
            val page = getNextPage(cid)
            val result = loadNextSystemPage(cid, page)
            if (!result.hasMore && result.articles.isEmpty()) {
                updatePageCont(page - 1, cid)
                _uiState.updateData { state ->
                    state.copy(
                        isLoading = state.isLoading + (cid to false),
                        isFinishing = state.isFinishing + (cid to true),
                    )
                }
                return@launch
            }
            updatePageCont(result.pageCount, cid)
            val hasNext = hasNextPage(cid)
            result.articles.onEach { it.preloadForDisplay() }
            extraPages.getOrPut(cid) { mutableListOf() }.addAll(result.articles)
            _uiState.updateData { state ->
                state.copy(
                    result = state.result + (cid to (state.result[cid].orEmpty() + result.articles)),
                    isLoading = state.isLoading + (cid to hasNext),
                    isFinishing = state.isFinishing + (cid to !hasNext),
                )
            }
        }
    }
}