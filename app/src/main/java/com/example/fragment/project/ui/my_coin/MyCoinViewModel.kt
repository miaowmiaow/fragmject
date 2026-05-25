package com.example.fragment.project.ui.my_coin

import androidx.lifecycle.viewModelScope
import com.example.fragment.project.data.Coin
import com.example.fragment.project.data.MyCoin
import com.example.fragment.project.data.MyCoinList
import com.example.fragment.project.data.UserCoin
import com.example.fragment.project.data.repository.MyRepository
import com.example.fragment.project.data.repository.WanRepositoryProvider
import com.example.miaow.base.vm.BaseViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MyCoinUiState(
    val isRefreshing: Boolean = false,
    val isLoading: Boolean = false,
    val isFinishing: Boolean = false,
    val userCoinResult: Coin = Coin(),
    val myCoinResult: List<MyCoin> = emptyList(),
)

class MyCoinViewModel(
    private val myRepo: MyRepository = WanRepositoryProvider.my,
) : BaseViewModel() {

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
            val userCoin: UserCoin = async { myRepo.getUserCoin() }.await()
            val myCoinList: MyCoinList = async { myRepo.getMyCoinList(getHomePage(1)) }.await()
            _uiState.update { state ->
                state.copy(
                    isRefreshing = false,
                    isLoading = hasNextPage(),
                    userCoinResult = userCoin.data ?: state.userCoinResult,
                    myCoinResult = myCoinList.data?.datas?.toList() ?: emptyList(),
                )
            }
        }
    }

    fun getNext() {
        _uiState.update {
            it.copy(isRefreshing = false, isLoading = false, isFinishing = false)
        }
        viewModelScope.launch {
            val response = myRepo.getMyCoinList(getNextPage())
            updatePageCont(response.data?.pageCount?.toInt())
            _uiState.update { state ->
                val datas = response.data?.datas.orEmpty()
                state.copy(
                    isRefreshing = false,
                    isLoading = hasNextPage(),
                    isFinishing = !hasNextPage(),
                    myCoinResult = if (datas.isEmpty()) state.myCoinResult else state.myCoinResult + datas
                )
            }
        }
    }

}