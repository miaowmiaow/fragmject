package com.example.fragment.project.ui.my_coin

import androidx.lifecycle.viewModelScope
import com.example.fragment.library.base.http.HttpRequest
import com.example.fragment.library.base.http.get
import com.example.fragment.library.base.vm.BaseViewModel
import com.example.fragment.project.bean.CoinBean
import com.example.fragment.project.bean.MyCoinBean
import com.example.fragment.project.bean.MyCoinListBean
import com.example.fragment.project.bean.UserCoinBean
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MyCoinState(
    var refreshing: Boolean = false,
    var loading: Boolean = false,
    var userCoinResult: CoinBean = CoinBean(),
    var myCoinResult: MutableList<MyCoinBean> = ArrayList(),
)

class MyCoinViewModel : BaseViewModel() {

    private val _uiState = MutableStateFlow(MyCoinState())

    val uiState: StateFlow<MyCoinState> = _uiState.asStateFlow()

    init {
        getHome()
    }

    fun getHome() {
        _uiState.update {
            it.copy(refreshing = true)
        }
        viewModelScope.launch {
            //通过async获取需要展示的数据
            val getUserCoin = async { getUserCoin() }
            val getMyCoinList = async { getMyCoinList(getHomePage(1)) }
            val userCoin = getUserCoin.await()
            val myCoinList = getMyCoinList.await()
            _uiState.update { state ->
                userCoin.data?.let { data ->
                    state.userCoinResult = data
                }
                myCoinList.data?.datas?.let { data ->
                    state.myCoinResult.clear()
                    state.myCoinResult.addAll(data)
                }
                state.copy(refreshing = false, loading = hasNextPage())
            }
        }
    }

    fun getNext() {
        _uiState.update {
            it.copy(loading = false)
        }
        viewModelScope.launch {
            val response = getMyCoinList(getNextPage())
            _uiState.update { state ->
                response.data?.datas?.let { data ->
                    state.myCoinResult.addAll(data)
                }
                state.copy(refreshing = false, loading = hasNextPage())
            }
        }
    }

    /**
     * 获取个人积分获取列表
     * page 1开始
     */
    private suspend fun getMyCoinList(page: Int): MyCoinListBean {
        //构建请求体，传入请求参数
        val request = HttpRequest("lg/coin/list/{page}/json").putPath("page", page.toString())
        //以get方式发起网络请求
        val response = coroutineScope { get<MyCoinListBean>(request) }
        //根据接口返回更新总页码
        updatePageCont(response.data?.pageCount?.toInt())
        return response
    }

    /**
     * 获取个人积分
     */
    private suspend fun getUserCoin(): UserCoinBean {
        return coroutineScope { get(HttpRequest("lg/coin/userinfo/json")) }
    }

}