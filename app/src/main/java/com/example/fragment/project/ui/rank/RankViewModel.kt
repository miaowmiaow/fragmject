package com.example.fragment.project.ui.rank

import androidx.lifecycle.viewModelScope
import com.example.fragment.project.data.Coin
import com.example.fragment.project.data.CoinRank
import com.example.miaow.base.http.get
import com.example.miaow.base.vm.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RankUiState(
    var isRefreshing: Boolean = false,
    var isLoading: Boolean = false,
    var isFinishing: Boolean = false,
    var result: MutableList<Coin> = ArrayList(),
)

class RankViewModel : BaseViewModel() {

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
            val response = get<CoinRank> {
                setUrl("coin/rank/{page}/json")
                putPath("page", page.toString())
            }
            updatePageCont(response.data?.pageCount?.toInt())
            _uiState.update { state ->
                response.data?.datas?.let { data ->
                    if (isHomePage()) {
                        state.result.clear()
                    }
                    state.result.addAll(data)
                }
                state.copy(
                    isRefreshing = false,
                    isLoading = hasNextPage(),
                    isFinishing = !hasNextPage()
                )
            }
        }
    }

}