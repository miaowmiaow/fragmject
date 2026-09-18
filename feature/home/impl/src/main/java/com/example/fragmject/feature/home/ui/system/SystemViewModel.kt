package com.example.fragmject.feature.home.ui.system

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.example.fragmject.core.model.Article
import com.example.fragmject.core.model.Tree
import com.example.fragmject.core.domain.repository.NavigationRepository
import com.example.fragmject.core.domain.result.DomainResult
import com.example.fragmject.core.domain.usecase.CollectArticleUseCase
import com.example.fragmject.core.domain.usecase.RefreshSystemArticlesUseCase
import com.example.fragmject.core.domain.usecase.LoadNextSystemPageUseCase
import com.example.fragmject.core.domain.usecase.ObserveSystemArticlesUseCase
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

private const val TAG = "SystemVM"

@HiltViewModel
class SystemViewModel @Inject constructor(
    private val observeSystemArticles: ObserveSystemArticlesUseCase,
    private val refreshSystemArticles: RefreshSystemArticlesUseCase,
    private val loadNextSystemPage: LoadNextSystemPageUseCase,
    private val collectArticle: CollectArticleUseCase,
    private val navigationRepository: NavigationRepository,
) : BaseViewModel(), CollectActionHolder {

    private val _uiState = MutableStateFlow<SystemUiState>(SystemUiState.Success())
    val uiState: StateFlow<SystemUiState> = _uiState.asStateFlow()

    /** 体系树（与 MainViewModel 共享同一 Room 数据源，供 SystemScreen 定位 cid）。 */
    private val _treeResult = MutableStateFlow<List<Tree>>(emptyList())
    val treeResult: StateFlow<List<Tree>> = _treeResult.asStateFlow()

    /** 每个 cid 的页面 1+ 内存列表。 */
    private val extraPages = ConcurrentHashMap<String, MutableList<Article>>()

    init {
        viewModelScope.launch {
            navigationRepository.observeSystemTree().collect { trees ->
                _treeResult.value = trees
            }
        }
    }

    fun init(cid: String) {
        if (!uiState.value.result.containsKey(cid)) {
            // 观察 Room 中第 0 页（system_{cid}_0）
            viewModelScope.launch {
                observeSystemArticles(cid).collect { page0 ->
                    _uiState.updateSuccessFrom({ SystemUiState.Success() }) { state ->
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

    fun getHome(cid: String, userTriggered: Boolean = false) {
        extraPages.remove(cid)

        viewModelScope.launch {
            getHomePage(0, cid)
            _uiState.updateSuccessFrom({ SystemUiState.Success() }) { state ->
                state.copy(isRefreshing = state.isRefreshing + (cid to userTriggered))
            }
            val pageCount = when (val r = refreshSystemArticles(cid)) {
                is DomainResult.Success -> r.data
                is DomainResult.Failure -> {
                    Log.e(TAG, "refreshSystemArticles failed cid=$cid: ${r.code} ${r.message}")
                    null
                }
            }
            val paging = finishPage(pageCount, cid)
            _uiState.updateSuccessFrom({ SystemUiState.Success() }) { state ->
                state.copy(
                    isRefreshing = state.isRefreshing + (cid to false),
                    isLoading = state.isLoading + (cid to paging.isLoading),
                    isFinishing = state.isFinishing + (cid to paging.isFinishing),
                )
            }
        }
    }

    fun getNext(cid: String) {
        _uiState.updateSuccessFrom({ SystemUiState.Success() }) { state ->
            state.copy(isLoading = state.isLoading + (cid to false))
        }
        viewModelScope.launch {
            val result = loadNextPage(key = cid) { page ->
                when (val r = loadNextSystemPage(cid, page)) {
                    is DomainResult.Success -> {
                        if (r.data.articles.isEmpty()) {
                            PageResult(success = false)
                        } else {
                            PageResult(items = r.data.articles, pageCount = r.data.pageCount)
                        }
                    }
                    is DomainResult.Failure -> {
                        Log.e(TAG, "loadNextSystemPage failed cid=$cid: ${r.code} ${r.message}")
                        PageResult(success = false)
                    }
                }
            }
            if (!result.success) {
                _uiState.updateSuccessFrom({ SystemUiState.Success() }) { state ->
                    state.copy(
                        isLoading = state.isLoading + (cid to false),
                        isFinishing = state.isFinishing + (cid to true),
                    )
                }
                return@launch
            }
            extraPages.getOrPut(cid) { mutableListOf() }.addAll(result.items)
            _uiState.updateSuccessFrom({ SystemUiState.Success() }) { state ->
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