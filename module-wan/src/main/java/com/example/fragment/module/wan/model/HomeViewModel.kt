package com.example.fragment.module.wan.model

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.fragment.library.base.http.HttpRequest
import com.example.fragment.library.base.http.get
import com.example.fragment.library.base.model.BaseViewModel
import com.example.fragment.library.common.bean.ArticleBean
import com.example.fragment.library.common.bean.ArticleListBean
import com.example.fragment.library.common.bean.BannerDataBean
import com.example.fragment.library.common.bean.TopArticleBean
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

class HomeViewModel : BaseViewModel() {

    val bannerResult = MutableLiveData<BannerDataBean>()
    val articleListResult = MutableLiveData<List<ArticleBean>>()

    fun getBanner() {
        viewModelScope.launch {
            val request = HttpRequest("banner/json")
            val response = get<BannerDataBean>(request)
            bannerResult.postValue(response)
        }
    }

    fun getArticle() {
        viewModelScope.launch {
            val list = ArrayList<ArticleBean>()
            val value1 = async { getArticleTop() }
            val value2 = async { getArticleList(getHomePage()) }
            value1.await().data?.onEach {
                it.top = true
            }?.let {
                list.addAll(it)
            }
            value2.await().data?.datas?.let {
                list.addAll(it)
            }
            articleListResult.postValue(list)
        }
    }

    fun getArticleNext() {
        viewModelScope.launch {
            getArticleList(getNextPage()).data?.datas?.let {
                articleListResult.postValue(it)
            }
        }
    }

    private suspend fun getArticleTop(): TopArticleBean {
        val request = HttpRequest("article/top/json")
        return coroutineScope { get(request) }
    }

    private suspend fun getArticleList(page: Int): ArticleListBean {
        val request = HttpRequest("article/list/{page}/json")
        request.putPath("page", page.toString())
        val response = coroutineScope { get<ArticleListBean>(request) { progress(it) } }
        response.data?.pageCount?.let { updatePageCont(it.toInt()) }
        return response
    }

}