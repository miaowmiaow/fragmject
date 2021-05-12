package com.example.fragment.user.model

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.fragment.library.base.http.HttpRequest
import com.example.fragment.library.base.http.get
import com.example.fragment.library.common.model.BaseViewModel
import com.example.fragment.user.bean.MyCoinListBean
import com.example.fragment.user.bean.UserCoinBean
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CoinModel : BaseViewModel() {

    val userCoinResult = MutableLiveData<UserCoinBean>()
    val myCoinListResult = MutableLiveData<MyCoinListBean>()
    var page = 1
    var pageCont = 1
    var isRefresh = true

    fun getUserCoin(){
        viewModelScope.launch(Dispatchers.Main) {
            userCoinResult.postValue(get(HttpRequest("lg/coin/userinfo/json")))
        }
    }

    fun getMyCoinList(isRefresh: Boolean) {
        this.isRefresh = isRefresh
        viewModelScope.launch(Dispatchers.Main) {
            if (isRefresh) {
                page = 1
            } else {
                page++
            }
            if (page <= pageCont) {
                val request = HttpRequest("lg/coin/list/{page}/json")
                request.putPath("page", page.toString())
                val result = get<MyCoinListBean>(request)
                result.data?.pageCount?.let { pageCont = it.toInt() }
                myCoinListResult.postValue(result)
            }
        }
    }

}