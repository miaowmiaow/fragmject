package com.example.fragment.module.home.model

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.fragment.library.base.http.HttpRequest
import com.example.fragment.library.base.http.get
import com.example.fragment.library.base.http.post
import com.example.fragment.library.common.bean.*
import com.example.fragment.library.common.model.BaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HomeViewModel : BaseViewModel() {

    val bannerResult = MutableLiveData<BannerDataBean>()
    val articleTopResult = MutableLiveData<TopArticleBean>()
    val articleListResult = MutableLiveData<ArticleListBean>()
    val searchResult = MutableLiveData<ArticleListBean>()
    val userArticleResult = MutableLiveData<ArticleListBean>()

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

    fun search(isRefresh: Boolean, k: String) {
        this.isRefresh = isRefresh
        viewModelScope.launch(Dispatchers.Main) {
            if (isRefresh) {
                page = 0
            } else {
                page++
            }
            if (page <= pageCont) {
                val request = HttpRequest("article/query/{page}/json")
                request.putPath("page", page.toString())
                request.putParam("k", k)
                val result = post<ArticleListBean>(request)
                result.data?.pageCount?.let { pageCont = it.toInt() }
                searchResult.postValue(result)
            }
        }
    }

    fun getUserArticleList(isRefresh: Boolean) {
        this.isRefresh = isRefresh
        viewModelScope.launch(Dispatchers.Main) {
            if (isRefresh) {
                page = 0
            } else {
                page++
            }
            if (page <= pageCont) {
                val request = HttpRequest("user_article/list/{page}/json")
                request.putPath("page", page.toString())
                val result = get<ArticleListBean>(request)
                result.data?.pageCount?.let { pageCont = it.toInt() }
                userArticleResult.postValue(result)
            }
        }
    }

}