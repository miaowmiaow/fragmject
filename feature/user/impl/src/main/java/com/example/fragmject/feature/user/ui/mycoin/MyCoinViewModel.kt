package com.example.fragmject.feature.user.ui.mycoin

import androidx.lifecycle.viewModelScope
import com.example.fragmject.core.model.Coin
import com.example.fragmject.core.model.MyCoin
import com.example.fragmject.core.domain.usecase.RefreshMyCoinUseCase
import com.example.fragmject.core.domain.usecase.LoadNextMyCoinPageUseCase
import com.example.fragmject.core.common.viewmodel.BaseViewModel
import com.example.fragmject.core.common.viewmodel.updateSuccessFrom
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

@HiltViewModel
class MyCoinViewModel @Inject constructor(
    private val refreshMyCoin: RefreshMyCoinUseCase,
    private val loadNextMyCoinPage: LoadNextMyCoinPageUseCase,
) : BaseViewModel() {

    private val _uiState = MutableStateFlow<MyCoinUiState>(MyCoinUiState.Success())
    val uiState: StateFlow<MyCoinUiState> = _uiState.asStateFlow()

    init { getHome() }

    fun getHome(userTriggered: Boolean = false) {
        viewModelScope.launch {
            _uiState.updateSuccessFrom({ MyCoinUiState.Success() }) { it.copy(isRefreshing = userTriggered, isLoading = false, isFinishing = false) }
            val result = refreshMyCoin(getHomePage(1))
            val paging = finishPage(result.pageCount)
            _uiState.updateSuccessFrom({ MyCoinUiState.Success() }) { state ->
                state.copy(
                    isRefreshing = false,
                    isLoading = paging.isLoading,
                    isFinishing = paging.isFinishing,
                    userCoinResult = result.coin ?: state.userCoinResult,
                    myCoinResult = result.coinList,
                )
            }
        }
    }

    fun getNext() {
        _uiState.updateSuccessFrom({ MyCoinUiState.Success() }) { it.copy(isRefreshing = false, isLoading = false, isFinishing = false) }
        viewModelScope.launch {
            val result = loadNextMyCoinPage(getNextPage())
            if (result.isEmpty) {
                _uiState.updateSuccessFrom({ MyCoinUiState.Success() }) { it.copy(isLoading = false, isFinishing = true) }
                return@launch
            }
            val paging = finishPage(result.pageCount)
            _uiState.updateSuccessFrom({ MyCoinUiState.Success() }) { state ->
                state.copy(
                    isRefreshing = false,
                    isLoading = paging.isLoading,
                    isFinishing = paging.isFinishing,
                    myCoinResult = state.myCoinResult + result.items,
                )
            }
        }
    }
}