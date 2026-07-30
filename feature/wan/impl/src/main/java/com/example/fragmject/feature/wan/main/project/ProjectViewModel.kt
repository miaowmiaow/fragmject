package com.example.fragmject.feature.wan.main.project

import androidx.lifecycle.viewModelScope
import com.example.fragmject.core.model.Article
import com.example.fragmject.core.data.repository.OfflineFirstProjectRepository
import com.example.fragmject.core.common.viewmodel.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

sealed interface ProjectUiState {
    data class Success(
        val isRefreshing: Map<String, Boolean> = emptyMap(),
        val isLoading: Map<String, Boolean> = emptyMap(),
        val isFinishing: Map<String, Boolean> = emptyMap(),
        val result: Map<String, List<Article>> = emptyMap(),
    ) : ProjectUiState

    fun getRefreshing(cid: String): Boolean = (this as? Success)?.isRefreshing?.get(cid) ?: false
    fun getLoading(cid: String): Boolean = (this as? Success)?.isLoading?.get(cid) ?: false
    fun getFinishing(cid: String): Boolean = (this as? Success)?.isFinishing?.get(cid) ?: false
    fun getResult(cid: String): List<Article>? = (this as? Success)?.result?.get(cid)
}

val ProjectUiState.isRefreshing get() = (this as? ProjectUiState.Success)?.isRefreshing ?: emptyMap()
val ProjectUiState.isLoading get() = (this as? ProjectUiState.Success)?.isLoading ?: emptyMap()
val ProjectUiState.isFinishing get() = (this as? ProjectUiState.Success)?.isFinishing ?: emptyMap()
val ProjectUiState.result get() = (this as? ProjectUiState.Success)?.result ?: emptyMap()

private inline fun MutableStateFlow<ProjectUiState>.updateData(
    crossinline block: (ProjectUiState.Success) -> ProjectUiState.Success,
) {
    update {
        val current = (it as? ProjectUiState.Success) ?: ProjectUiState.Success()
        block(current)
    }
}

@HiltViewModel
class ProjectViewModel @Inject constructor(
    private val repo: OfflineFirstProjectRepository,
) : BaseViewModel() {

    private val _uiState = MutableStateFlow<ProjectUiState>(ProjectUiState.Success())
    val uiState: StateFlow<ProjectUiState> = _uiState.asStateFlow()

    /** 每个 cid 的页面 2+ 内存列表。 */
    private val extraPages = ConcurrentHashMap<String, MutableList<Article>>()

    fun init(cid: String) {
        if (!uiState.value.result.containsKey(cid)) {
            // 观察 Room 中第 1 页（project_{cid}_1）
            viewModelScope.launch {
                repo.observeProjectArticles(cid).collect { page1 ->
                    page1.onEach { it.preloadForDisplay() }
                    _uiState.updateData { state ->
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

        _uiState.updateData { state ->
            state.copy(isRefreshing = state.isRefreshing + (cid to userTriggered))
        }
        viewModelScope.launch {
            getHomePage(1, cid)
            val pageCount = repo.refreshProjectArticles(cid)
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
            val data = repo.loadProjectNextPage(cid, page)
            if (data == null) {
                updatePageCont(page - 1, cid)
                _uiState.updateData { state ->
                    state.copy(
                        isLoading = state.isLoading + (cid to false),
                        isFinishing = state.isFinishing + (cid to true),
                    )
                }
                return@launch
            }
            updatePageCont(data.pageCount, cid)
            val hasNext = hasNextPage(cid)
            data.articles.onEach { it.preloadForDisplay() }
            extraPages.getOrPut(cid) { mutableListOf() }.addAll(data.articles)
            _uiState.updateData { state ->
                state.copy(
                    result = state.result + (cid to (state.result[cid].orEmpty() + data.articles)),
                    isLoading = state.isLoading + (cid to hasNext),
                    isFinishing = state.isFinishing + (cid to !hasNext),
                )
            }
        }
    }
}