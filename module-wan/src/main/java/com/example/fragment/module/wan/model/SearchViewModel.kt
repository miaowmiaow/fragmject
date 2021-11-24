package com.example.fragment.module.wan.model

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.fragment.library.base.http.HttpRequest
import com.example.fragment.library.base.http.post
import com.example.fragment.library.base.model.BaseViewModel
import com.example.fragment.library.common.bean.ArticleListBean
import kotlinx.coroutines.launch

class SearchViewModel : BaseViewModel() {

    val searchResult = MutableLiveData<ArticleListBean>()

    fun getSearch(k: String) {
        getArticleQuery(k, getHomePage())
    }

    fun getSearchNext(k: String) {
        getArticleQuery(k, getNextPage())
    }

    private fun getArticleQuery(k: String, page: Int) {
        viewModelScope.launch {
            val request = HttpRequest("article/query/{page}/json")
            request.putParam("k", k)
            request.putPath("page", page.toString())
            val result = post<ArticleListBean>(request) { progress(it) }
            result.data?.pageCount?.let { updatePageCont(it.toInt()) }
            searchResult.postValue(result)
        }
    }

}