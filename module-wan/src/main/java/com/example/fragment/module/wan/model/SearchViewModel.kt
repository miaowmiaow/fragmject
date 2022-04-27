package com.example.fragment.module.wan.model

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.fragment.library.base.http.HttpRequest
import com.example.fragment.library.base.http.post
import com.example.fragment.library.base.model.BaseViewModel
import com.example.fragment.library.common.bean.ArticleListBean
import com.example.fragment.library.common.utils.WanHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SearchViewModel : BaseViewModel() {

    private val searchHistoryResult: MutableLiveData<List<String>> by lazy {
        MutableLiveData<List<String>>().also {
            getSearchHistory()
        }
    }

    fun searchHistoryResult(): LiveData<List<String>> {
        return searchHistoryResult
    }

    fun getSearchHistory() {
        WanHelper.getSearchHistory { searchHistoryResult.postValue(it) }
    }

    private var key: String = ""

    private val articleQueryResult: MutableLiveData<ArticleListBean> by lazy {
        MutableLiveData<ArticleListBean>().also {
            getArticleQueryHome(key)
        }
    }

    fun articleQueryResult(key: String): LiveData<ArticleListBean> {
        this.key = key
        return articleQueryResult
    }

    fun getArticleQueryHome(key: String) {
        this.key = key
        getArticleQuery(key, getHomePage())
    }

    fun getArticleQueryNext(key: String) {
        this.key = key
        getArticleQuery(key, getNextPage())
    }

    /**
     * 搜索
     * k 搜索关键词
     * page 0开始
     */
    private fun getArticleQuery(key: String, page: Int) {
        //通过viewModelScope创建一个协程
        viewModelScope.launch {
            //如果LiveData.value == null，则在转场动画结束后加载数据，用于解决过度动画卡顿问题
            if (articleQueryResult.value == null) {
                delay(LOAD_DELAY_MILLIS)
            }
            //构建请求体，传入请求参数
            val request = HttpRequest("article/query/{page}/json")
                .putParam("k", key)
                .putPath("page", page.toString())
            //以get方式发起网络请求
            val result = post<ArticleListBean>(request)
            //根据接口返回更新总页码
            result.data?.pageCount?.let { updatePageCont(it.toInt()) }
            //通过LiveData通知界面更新
            articleQueryResult.postValue(result)
        }
    }

}