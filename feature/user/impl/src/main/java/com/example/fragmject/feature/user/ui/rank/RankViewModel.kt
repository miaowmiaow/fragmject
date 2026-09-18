package com.example.fragmject.feature.user.ui.rank

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.example.fragmject.core.model.Coin
import com.example.fragmject.core.domain.repository.CoinRankRepository
import com.example.fragmject.core.domain.result.DomainResult
import com.example.fragmject.core.common.viewmodel.BaseViewModel
import com.example.fragmject.core.common.viewmodel.PageResult
import com.example.fragmject.core.common.viewmodel.updateSuccessFrom
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

private const val TAG = "RankVM"

@HiltViewModel
class RankViewModel @Inject constructor(
    private val repo: CoinRankRepository,
) : BaseViewModel() {

    private val _uiState = MutableStateFlow<RankUiState>(RankUiState.Success())
    val uiState: StateFlow<RankUiState> = _uiState.asStateFlow()

    /** 页面 2+ 的内存列表。 */
    private val extraPages = mutableListOf<Coin>()

    init {
        // 观察 Room 中第 1 页
        viewModelScope.launch {
            repo.observeCoinRanks().collect { page1 ->
                _uiState.updateSuccessFrom({ RankUiState.Success() }) {
                    it.copy(isRefreshing = false, result = page1 + extraPages)
                }
            }
        }
        getHome()
    }

    fun getHome(userTriggered: Boolean = false) {
        extraPages.clear()

        _uiState.updateSuccessFrom({ RankUiState.Success() }) { it.copy(isRefreshing = userTriggered) }
        viewModelScope.launch {
            getHomePage(1)
            val pageCount = when (val r = repo.refreshCoinRank()) {
                is DomainResult.Success -> r.data
                is DomainResult.Failure -> {
                    Log.e(TAG, "refreshCoinRank failed: ${r.code} ${r.message}")
                    null
                }
            }
            updatePageCont(pageCount)
            val hasNext = hasNextPage()
            _uiState.updateSuccessFrom({ RankUiState.Success() }) {
                it.copy(isRefreshing = false, isLoading = hasNext, isFinishing = !hasNext)
            }
        }
    }

    fun getNext() {
        _uiState.updateSuccessFrom({ RankUiState.Success() }) { it.copy(isLoading = false) }
        viewModelScope.launch {
            val result = loadNextPage { page ->
                when (val r = repo.loadCoinRankNextPage(page)) {
                    is DomainResult.Success -> {
                        if (r.data.coins.isEmpty()) {
                            PageResult(success = false)
                        } else {
                            PageResult(items = r.data.coins, pageCount = r.data.pageCount)
                        }
                    }
                    is DomainResult.Failure -> {
                        Log.e(TAG, "loadCoinRankNextPage failed: ${r.code} ${r.message}")
                        PageResult(success = false)
                    }
                }
            }
            if (!result.success) {
                _uiState.updateSuccessFrom({ RankUiState.Success() }) { it.copy(isLoading = false, isFinishing = true) }
                return@launch
            }
            extraPages.addAll(result.items)
            _uiState.updateSuccessFrom({ RankUiState.Success() }) {
                it.copy(
                    result = it.result + result.items,
                    isLoading = result.hasMore,
                    isFinishing = !result.hasMore,
                )
            }
        }
    }
}