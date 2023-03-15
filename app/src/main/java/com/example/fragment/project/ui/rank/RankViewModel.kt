package com.example.fragment.project.ui.rank

import androidx.lifecycle.viewModelScope
import com.example.fragment.library.base.http.HttpRequest
import com.example.fragment.library.base.http.get
import com.example.fragment.library.base.vm.BaseViewModel
import com.example.fragment.project.bean.CoinBean
import com.example.fragment.project.bean.CoinRankBean
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RankState(
    var refreshing: Boolean = false,
    var loading: Boolean = false,
    var result: MutableList<CoinBean> = ArrayList(),
)

class RankViewModel : BaseViewModel() {

    private val _uiState = MutableStateFlow(RankState())

    val uiState: StateFlow<RankState> = _uiState.asStateFlow()

    init {
        getHome()
    }

    fun getHome() {
        _uiState.update { it.copy(refreshing = true) }
        getCoinRank(getHomePage(1))
    }

    fun getNext() {
        _uiState.update { it.copy(loading = false) }
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
            response.data?.pageCount?.let { updatePageCont(it.toInt()) }
            _uiState.update {
                response.data?.datas?.let { data ->
                    if (isHomePage()) {
                        it.result.clear()
                    }
                    it.result.addAll(data)
                }
                it.copy(refreshing = false, loading = hasNextPage())
            }
        }
    }

}