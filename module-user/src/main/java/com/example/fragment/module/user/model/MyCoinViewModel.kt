package com.example.fragment.module.user.model

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.fragment.library.base.http.HttpRequest
import com.example.fragment.library.base.http.get
import com.example.fragment.library.base.model.BaseViewModel
import com.example.fragment.library.common.bean.MyCoinListBean
import com.example.fragment.library.common.bean.UserCoinBean
import kotlinx.coroutines.launch

class MyCoinViewModel : BaseViewModel() {

    val userCoinResult = MutableLiveData<UserCoinBean>()
    val myCoinResult = MutableLiveData<MyCoinListBean>()

    fun userCoin() {
        viewModelScope.launch {
            val request = HttpRequest("lg/coin/userinfo/json")
            val response = get<UserCoinBean>(request)
            userCoinResult.postValue(response)
        }
    }

    fun getMyCoin(){
        getMyCoin(getHomePage(1))
    }

    fun getMyCoinNext(){
        getMyCoin(getNextPage())
    }

    private fun getMyCoin(page: Int) {
        viewModelScope.launch {
            val request = HttpRequest("lg/coin/list/{page}/json")
            request.putPath("page", page.toString())
            val response = get<MyCoinListBean>(request) { progress(it) }
            response.data?.pageCount?.let { updatePageCont(it.toInt()) }
            myCoinResult.postValue(response)
        }
    }

}