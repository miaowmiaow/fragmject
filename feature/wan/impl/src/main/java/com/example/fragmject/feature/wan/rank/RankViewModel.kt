package com.example.fragmject.feature.wan.rank

import androidx.lifecycle.viewModelScope
import com.example.fragmject.core.model.Coin
import com.example.fragmject.core.data.repository.OfflineFirstCoinRankRepository
import com.example.fragmject.core.common.viewmodel.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface RankUiState {
    data class Success(
        val isRefreshing: Boolean = false,
        val isLoading: Boolean = false,
        val isFinishing: Boolean = false,
        val result: List<Coin> = emptyList(),
    ) : RankUiState
}

val RankUiState.isRefreshing get() = (this as? RankUiState.Success)?.isRefreshing ?: false
val RankUiState.isLoading get() = (this as? RankUiState.Success)?.isLoading ?: false
val RankUiState.isFinishing get() = (this as? RankUiState.Success)?.isFinishing ?: false
val RankUiState.result get() = (this as? RankUiState.Success)?.result ?: emptyList()

private inline fun MutableStateFlow<RankUiState>.updateData(
    crossinline block: (RankUiState.Success) -> RankUiState.Success,
) {
    update {
        val current = (it as? RankUiState.Success) ?: RankUiState.Success()
        block(current)
    }
}

@HiltViewModel
class RankViewModel @Inject constructor(
    private val repo: OfflineFirstCoinRankRepository,
) : BaseViewModel() {

    private val _uiState = MutableStateFlow<RankUiState>(RankUiState.Success())
    val uiState: StateFlow<RankUiState> = _uiState.asStateFlow()

    /** 页面 2+ 的内存列表。 */
    private val extraPages = mutableListOf<Coin>()

    init {
        // 观察 Room 中第 1 页
        viewModelScope.launch {
            repo.observeCoinRanks().collect { page1 ->
                _uiState.updateData {
                    it.copy(isRefreshing = false, result = page1 + extraPages)
                }
            }
        }
        getHome()
    }

    fun getHome() {
        extraPages.clear()

        _uiState.updateData { it.copy(isRefreshing = true) }
        viewModelScope.launch {
            getHomePage(1)
            val pageCount = repo.refreshCoinRank()
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
            val data = repo.loadCoinRankNextPage(page)
            if (data == null) {
                updatePageCont(page - 1)
                _uiState.updateData { it.copy(isLoading = false, isFinishing = true) }
                return@launch
            }
            updatePageCont(data.pageCount)
            val hasNext = hasNextPage()
            extraPages.addAll(data.coins)
            _uiState.updateData {
                it.copy(
                    result = it.result + data.coins,
                    isLoading = hasNext,
                    isFinishing = !hasNext,
                )
            }
        }
    }
}