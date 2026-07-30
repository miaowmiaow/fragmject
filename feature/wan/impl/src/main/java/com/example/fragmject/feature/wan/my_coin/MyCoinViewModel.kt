package com.example.fragmject.feature.wan.my_coin

import androidx.lifecycle.viewModelScope
import com.example.fragmject.core.model.Coin
import com.example.fragmject.core.model.MyCoin
import com.example.fragmject.core.domain.usecase.RefreshMyCoinUseCase
import com.example.fragmject.core.domain.usecase.LoadNextMyCoinPageUseCase
import com.example.fragmject.core.common.TransitionGuard
import com.example.fragmject.core.common.viewmodel.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface MyCoinUiState {
    data class Success(
        val isRefreshing: Boolean = false,
        val isLoading: Boolean = false,
        val isFinishing: Boolean = false,
        val userCoinResult: Coin = Coin(),
        val myCoinResult: List<MyCoin> = emptyList(),
    ) : MyCoinUiState
}

val MyCoinUiState.isRefreshing get() = (this as? MyCoinUiState.Success)?.isRefreshing ?: false
val MyCoinUiState.isLoading get() = (this as? MyCoinUiState.Success)?.isLoading ?: false
val MyCoinUiState.isFinishing get() = (this as? MyCoinUiState.Success)?.isFinishing ?: false
val MyCoinUiState.userCoinResult get() = (this as? MyCoinUiState.Success)?.userCoinResult ?: Coin()
val MyCoinUiState.myCoinResult get() = (this as? MyCoinUiState.Success)?.myCoinResult ?: emptyList()

private inline fun MutableStateFlow<MyCoinUiState>.updateData(crossinline block: (MyCoinUiState.Success) -> MyCoinUiState.Success) {
    update { if (it is MyCoinUiState.Success) block(it) else it }
}

@HiltViewModel
class MyCoinViewModel @Inject constructor(
    private val refreshMyCoin: RefreshMyCoinUseCase,
    private val loadNextMyCoinPage: LoadNextMyCoinPageUseCase,
) : BaseViewModel() {

    private val _uiState = MutableStateFlow<MyCoinUiState>(MyCoinUiState.Success())
    val uiState: StateFlow<MyCoinUiState> = _uiState.asStateFlow()

    init { getHome() }

    fun getHome() {
        viewModelScope.launch {
            _uiState.updateData { it.copy(isRefreshing = true, isLoading = false, isFinishing = false) }
            val t = TransitionGuard.now()
            val result = refreshMyCoin(getHomePage(1))
            TransitionGuard.await(t)
            updatePageCont(result.pageCount)
            val hasNext = hasNextPage()
            _uiState.updateData { state ->
                state.copy(
                    isRefreshing = false,
                    isLoading = hasNext,
                    isFinishing = !hasNext,
                    userCoinResult = result.coin ?: state.userCoinResult,
                    myCoinResult = result.coinList,
                )
            }
        }
    }

    fun getNext() {
        _uiState.updateData { it.copy(isRefreshing = false, isLoading = false, isFinishing = false) }
        viewModelScope.launch {
            val result = loadNextMyCoinPage(getNextPage())
            if (result.isEmpty) {
                _uiState.updateData { it.copy(isLoading = false, isFinishing = true) }
                return@launch
            }
            updatePageCont(result.pageCount)
            val hasNext = hasNextPage()
            _uiState.updateData { state ->
                state.copy(
                    isRefreshing = false,
                    isLoading = hasNext,
                    isFinishing = !hasNext,
                    myCoinResult = state.myCoinResult + result.items,
                )
            }
        }
    }
}