package com.example.fragment.module.home.model

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.fragment.library.base.http.HttpRequest
import com.example.fragment.library.base.http.post
import com.example.fragment.library.common.bean.ArticleListBean
import com.example.fragment.library.common.model.BaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SearchViewModel : BaseViewModel() {

    val searchResult = MutableLiveData<ArticleListBean>()

    var page = 0
    var pageCont = 1
    var isRefresh = true

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

}