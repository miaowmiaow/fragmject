package com.example.fragment.module.wan.model

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.fragment.library.base.http.HttpRequest
import com.example.fragment.library.base.http.get
import com.example.fragment.library.base.model.BaseViewModel
import com.example.fragment.library.common.bean.ArticleListBean
import com.example.fragment.library.common.bean.BannerDataBean
import com.example.fragment.library.common.bean.TopArticleBean
import kotlinx.coroutines.launch

class HomeViewModel : BaseViewModel() {

    val bannerResult = MutableLiveData<BannerDataBean>()
    val articleTopResult = MutableLiveData<TopArticleBean>()
    val articleListResult = MutableLiveData<ArticleListBean>()

    fun getBanner() {
        viewModelScope.launch {
            val request = HttpRequest("banner/json")
            val response = get<BannerDataBean>(request)
            bannerResult.postValue(response)
        }
    }

    fun getArticleTop() {
        viewModelScope.launch {
            val request = HttpRequest("article/top/json")
            val response = get<TopArticleBean>(request)
            articleTopResult.postValue(response)
        }
    }

    fun getArticle(){
        getArticleList(getHomePage())
    }

    fun getArticleNext(){
        getArticleList(getNextPage())
    }

    private fun getArticleList(page: Int) {
        viewModelScope.launch {
            val request = HttpRequest("article/list/{page}/json")
            request.putPath("page", page.toString())
            val response = get<ArticleListBean>(request) { progress(it) }
            response.data?.pageCount?.let { updatePageCont(it.toInt()) }
            articleListResult.postValue(response)
        }
    }

}