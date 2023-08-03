package com.example.fragment.project.ui.rank

import androidx.lifecycle.viewModelScope
import com.example.fragment.project.bean.CoinBean
import com.example.fragment.project.bean.CoinRankBean
import com.example.miaow.base.http.HttpRequest
import com.example.miaow.base.http.get
import com.example.miaow.base.vm.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RankState(
    var refreshing: Boolean = false,
    var loading: Boolean = false,
    var finishing: Boolean = false,
    var result: MutableList<CoinBean> = ArrayList(),
)

class RankViewModel : BaseViewModel() {

    private val _uiState = MutableStateFlow(RankState())

    val uiState: StateFlow<RankState> = _uiState.asStateFlow()

    init {
        getHome()
    }

    fun getHome() {
        _uiState.update {
            it.copy(refreshing = true, loading = false, finishing = false)
        }
        getCoinRank(getHomePage(1))
    }

    fun getNext() {
        _uiState.update {
            it.copy(refreshing = false, loading = false, finishing = false)
        }
        getCoinRank(getNextPage())
    }

    /**
     * 获取积分排行榜
     * page 1开始
     */
    private fun getCoinRank(page: Int) {
        viewModelScope.launch {
            val request = HttpRequest("coin/rank/{page}/json").putPath("page", page.toString())
            val response = get<CoinRankBean>(request)
            updatePageCont(response.data?.pageCount?.toInt())
            _uiState.update { state ->
                response.data?.datas?.let { data ->
                    if (isHomePage()) {
                        state.result.clear()
                    }
                    state.result.addAll(data)
                }
                state.copy(refreshing = false, loading = hasNextPage(), finishing = !hasNextPage())
            }
        }
    }

}