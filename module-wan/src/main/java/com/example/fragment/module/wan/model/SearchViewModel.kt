package com.example.fragment.module.wan.model

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.fragment.library.base.http.HttpRequest
import com.example.fragment.library.base.http.post
import com.example.fragment.library.base.model.BaseViewModel
import com.example.fragment.library.common.bean.ArticleListBean
import com.example.fragment.library.common.utils.WanHelper
import kotlinx.coroutines.launch

class SearchViewModel : BaseViewModel() {

    val searchHistoryResult = MutableLiveData<List<String>>()
    val searchResult = MutableLiveData<ArticleListBean>()

    fun getSearchHistory() {
        WanHelper.getSearchHistory { searchHistoryResult.postValue(it) }
    }

    fun getSearch(k: String) {
        getArticleQuery(k, getHomePage())
    }

    fun getSearchNext(k: String) {
        getArticleQuery(k, getNextPage())
    }

    /**
     * 搜索
     * k 搜索关键词
     * page 0开始
     */
    private fun getArticleQuery(k: String, page: Int) {
        //通过viewModelScope创建一个协程
        viewModelScope.launch {
            //构建请求体，传入请求参数
            val request = HttpRequest("article/query/{page}/json")
                .putParam("k", k)
                .putPath("page", page.toString())
            //以get方式发起网络请求
            val result = post<ArticleListBean>(request) { progress(it) }
            //根据接口返回更新总页码
            result.data?.pageCount?.let { updatePageCont(it.toInt()) }
            //通过LiveData通知界面更新
            searchResult.postValue(result)
        }
    }

}