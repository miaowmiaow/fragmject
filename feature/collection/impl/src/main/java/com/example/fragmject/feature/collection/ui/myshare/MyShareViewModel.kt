package com.example.fragmject.feature.collection.ui.myshare

import androidx.lifecycle.viewModelScope
import com.example.fragmject.core.common.viewmodel.BaseViewModel
import com.example.fragmject.core.common.viewmodel.updateSuccessFrom
import com.example.fragmject.core.domain.usecase.CollectArticleUseCase
import com.example.fragmject.core.domain.usecase.LoadNextMySharePageUseCase
import com.example.fragmject.core.domain.usecase.RefreshMyShareUseCase
import com.example.fragmject.core.model.Article
import com.example.fragmject.core.common.CollectActionHolder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface MyShareUiState {
    data class Success(
        val isRefreshing: Boolean = false,
        val isLoading: Boolean = false,
        val isFinishing: Boolean = false,
        val result: List<Article> = emptyList(),
    ) : MyShareUiState
}


val MyShareUiState.isRefreshing get() = (this as? MyShareUiState.Success)?.isRefreshing ?: false
val MyShareUiState.isLoading get() = (this as? MyShareUiState.Success)?.isLoading ?: false
val MyShareUiState.isFinishing get() = (this as? MyShareUiState.Success)?.isFinishing ?: false
val MyShareUiState.result get() = (this as? MyShareUiState.Success)?.result ?: emptyList()

@HiltViewModel
class MyShareViewModel @Inject constructor(
    private val refreshMyShare: RefreshMyShareUseCase,
    private val loadNextMySharePage: LoadNextMySharePageUseCase,
    private val collectArticle: CollectArticleUseCase,
) : BaseViewModel(), CollectActionHolder {

    private val _uiState = MutableStateFlow<MyShareUiState>(MyShareUiState.Success())

    val uiState: StateFlow<MyShareUiState> = _uiState.asStateFlow()

    init {
        getHome()
    }

    fun getHome(userTriggered: Boolean = false) {
        val page = getHomePage()
        viewModelScope.launch {
            _uiState.updateSuccessFrom({ MyShareUiState.Success() }) { it.copy(isRefreshing = userTriggered, isLoading = false, isFinishing = false) }
            val result = refreshMyShare(page)
            updatePageCont(result.pageCount)
            _uiState.updateSuccessFrom({ MyShareUiState.Success() }) { state ->
                state.copy(
                    isRefreshing = false,
                    isLoading = result.hasMore,
                    isFinishing = !result.hasMore,
                    result = result.articles,
                )
            }
        }
    }

    fun getNext() {
        _uiState.updateSuccessFrom({ MyShareUiState.Success() }) { it.copy(isRefreshing = false, isLoading = false, isFinishing = false) }
        val page = getNextPage()
        viewModelScope.launch {
            val result = loadNextMySharePage(page)
            updatePageCont(result.pageCount)
            _uiState.updateSuccessFrom({ MyShareUiState.Success() }) { state ->
                val merged = if (isHomePage()) result.articles else state.result + result.articles
                state.copy(
                    isRefreshing = false,
                    isLoading = result.hasMore,
                    isFinishing = !result.hasMore,
                    result = merged,
                )
            }
        }
    }

    /** 收藏 / 取消收藏。 */
    override val collectAction: suspend (String, Boolean) -> Unit = { id, collect ->
        collectArticle(id, collect)
    }

}