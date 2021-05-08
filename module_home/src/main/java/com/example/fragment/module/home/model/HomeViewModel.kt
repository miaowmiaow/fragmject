package com.example.fragment.module.home.model

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.fragment.library.base.http.HttpRequest
import com.example.fragment.library.base.http.get
import com.example.fragment.library.common.bean.*
import com.example.fragment.library.common.model.BaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HomeViewModel : BaseViewModel() {

    val bannerResult = MutableLiveData<BannerDataBean>()
    val articleTopResult = MutableLiveData<TopArticleBean>()
    val articleListResult = MutableLiveData<ArticleListBean>()
    var page = 0
    var pageCont = 1
    var isRefresh = true

    fun getBanner() {
        viewModelScope.launch(Dispatchers.Main) {
            bannerResult.postValue(get(HttpRequest("banner/json")))
        }
    }

    fun getArticleList(isRefresh: Boolean) {
        this.isRefresh = isRefresh
        viewModelScope.launch(Dispatchers.Main) {
            if (isRefresh) {
                articleTopResult.postValue(get(HttpRequest("article/top/json")))
                page = 0
            } else {
                page++
            }
            if (page <= pageCont) {
                val request = HttpRequest("article/list/{page}/json")
                request.putPath("page", page.toString())
                val result = get<ArticleListBean>(request)
                result.data?.pageCount?.let { pageCont = it.toInt() }
                articleListResult.postValue(result)
            }
        }
    }

}