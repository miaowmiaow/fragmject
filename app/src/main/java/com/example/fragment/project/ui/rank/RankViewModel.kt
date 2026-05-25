package com.example.fragment.project.ui.rank

import androidx.lifecycle.viewModelScope
import com.example.fragment.project.data.Coin
import com.example.fragment.project.data.repository.CommonRepository
import com.example.fragment.project.data.repository.WanRepositoryProvider
import com.example.miaow.base.vm.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RankUiState(
    val isRefreshing: Boolean = false,
    val isLoading: Boolean = false,
    val isFinishing: Boolean = false,
    val result: List<Coin> = emptyList(),
)

class RankViewModel(
    private val commonRepo: CommonRepository = WanRepositoryProvider.common,
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(RankUiState())

    val uiState: StateFlow<RankUiState> = _uiState.asStateFlow()

    init {
        getHome()
    }

    fun getHome() {
        _uiState.update {
            it.copy(isRefreshing = true, isLoading = false, isFinishing = false)
        }
        getCoinRank(getHomePage(1))
    }

    fun getNext() {
        _uiState.update {
            it.copy(isRefreshing = false, isLoading = false, isFinishing = false)
        }
        getCoinRank(getNextPage())
    }

    /**
     * 获取积分排行榜
     * page 1开始
     */
    private fun getCoinRank(page: Int) {
        viewModelScope.launch {
            val response = commonRepo.getCoinRank(page)
            updatePageCont(response.data?.pageCount?.toInt())
            _uiState.update { state ->
                val datas = response.data?.datas.orEmpty()
                val merged = if (isHomePage()) datas else state.result + datas
                state.copy(
                    isRefreshing = false,
                    isLoading = hasNextPage(),
                    isFinishing = !hasNextPage(),
                    result = merged
                )
            }
        }
    }

}