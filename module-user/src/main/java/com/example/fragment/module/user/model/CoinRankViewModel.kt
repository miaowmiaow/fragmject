package com.example.fragment.module.user.model

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.fragment.library.base.http.HttpRequest
import com.example.fragment.library.base.http.get
import com.example.fragment.library.base.model.BaseViewModel
import com.example.fragment.library.common.bean.CoinRankBean
import kotlinx.coroutines.launch

class CoinRankViewModel : BaseViewModel() {

    val coinRankResult = MutableLiveData<CoinRankBean>()

    fun getCoinRank() {
        getCoinRank(getHomePage(1))
    }

    fun getCoinRankNext() {
        getCoinRank(getNextPage())
    }

    private fun getCoinRank(page: Int) {
        viewModelScope.launch {
            val request = HttpRequest("coin/rank/{page}/json")
            request.putPath("page", page.toString())
            val response = get<CoinRankBean>(request) { progress(it) }
            response.data?.pageCount?.let { updatePageCont(it.toInt()) }
            coinRankResult.postValue(response)
        }
    }

}