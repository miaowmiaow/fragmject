package com.example.fragment.project.ui.my_coin

import androidx.lifecycle.viewModelScope
import com.example.fragment.project.data.Coin
import com.example.fragment.project.data.MyCoin
import com.example.fragment.project.data.MyCoinList
import com.example.fragment.project.data.UserCoin
import com.example.miaow.base.http.get
import com.example.miaow.base.vm.BaseViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MyCoinUiState(
    var isRefreshing: Boolean = false,
    var isLoading: Boolean = false,
    var isFinishing: Boolean = false,
    var userCoinResult: Coin = Coin(),
    var myCoinResult: MutableList<MyCoin> = ArrayList(),
)

class MyCoinViewModel : BaseViewModel() {

    private val _uiState = MutableStateFlow(MyCoinUiState())

    val uiState: StateFlow<MyCoinUiState> = _uiState.asStateFlow()

    init {
        getHome()
    }

    fun getHome() {
        _uiState.update {
            it.copy(isRefreshing = true, isLoading = false, isFinishing = false)
        }
        viewModelScope.launch {
            //通过async获取需要展示的数据
            val userCoin = async { getUserCoin() }.await()
            val myCoinList = async { getMyCoinList(getHomePage(1)) }.await()
            _uiState.update { state ->
                userCoin.data?.let { data ->
                    state.userCoinResult = data
                }
                myCoinList.data?.datas?.let { data ->
                    state.myCoinResult.clear()
                    state.myCoinResult.addAll(data)
                }
                state.copy(isRefreshing = false, isLoading = hasNextPage())
            }
        }
    }

    fun getNext() {
        _uiState.update {
            it.copy(isRefreshing = false, isLoading = false, isFinishing = false)
        }
        viewModelScope.launch {
            val response = getMyCoinList(getNextPage())
            updatePageCont(response.data?.pageCount?.toInt())
            _uiState.update { state ->
                response.data?.datas?.let { data ->
                    state.myCoinResult.addAll(data)
                }
                state.copy(
                    isRefreshing = false,
                    isLoading = hasNextPage(),
                    isFinishing = !hasNextPage()
                )
            }
        }
    }

    /**
     * 获取个人积分获取列表
     * page 1开始
     */
    private suspend fun getMyCoinList(page: Int): MyCoinList {
        val response = coroutineScope {
            get<MyCoinList> {
                setUrl("lg/coin/list/{page}/json")
                putPath("page", page.toString())
            }
        }
        return response
    }

    /**
     * 获取个人积分
     */
    private suspend fun getUserCoin(): UserCoin {
        return coroutineScope {
            get {
                setUrl("lg/coin/userinfo/json")
            }
        }
    }

}