package com.example.fragmject.feature.wan.my_share

import androidx.lifecycle.viewModelScope
import com.example.fragmject.core.model.Article
import com.example.fragmject.core.data.repository.MyRepository
import com.example.fragmject.core.domain.usecase.RefreshMyShareUseCase
import com.example.fragmject.core.domain.usecase.LoadNextMySharePageUseCase
import com.example.fragmject.core.common.TransitionGuard
import com.example.fragmject.core.common.viewmodel.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
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

private inline fun MutableStateFlow<MyShareUiState>.updateData(
    crossinline block: (MyShareUiState.Success) -> MyShareUiState.Success,
) {
    update { if (it is MyShareUiState.Success) block(it) else it }
}
@HiltViewModel
class MyShareViewModel @Inject constructor(
    private val refreshMyShare: RefreshMyShareUseCase,
    private val loadNextMySharePage: LoadNextMySharePageUseCase,
) : BaseViewModel() {

    private val _uiState = MutableStateFlow<MyShareUiState>(MyShareUiState.Success())

    val uiState: StateFlow<MyShareUiState> = _uiState.asStateFlow()

    init {
        getHome()
    }

    fun getHome() {
        val page = getHomePage()
        viewModelScope.launch {
            _uiState.updateData { it.copy(isRefreshing = true, isLoading = false, isFinishing = false) }
            val t = TransitionGuard.now()
            val result = refreshMyShare(page)
            result.articles.onEach { it.preloadForDisplay() }
            TransitionGuard.await(t)
            updatePageCont(result.pageCount)
            _uiState.updateData { state ->
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
        _uiState.updateData { it.copy(isRefreshing = false, isLoading = false, isFinishing = false) }
        val page = getNextPage()
        viewModelScope.launch {
            val result = loadNextMySharePage(page)
            result.articles.onEach { it.preloadForDisplay() }
            updatePageCont(result.pageCount)
            _uiState.updateData { state ->
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

}