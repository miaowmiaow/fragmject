package com.example.fragment.module.wan.model

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fragment.library.base.http.HttpRequest
import com.example.fragment.library.base.http.HttpResponse
import com.example.fragment.library.base.http.get
import com.example.fragment.library.base.http.post
import com.example.fragment.library.common.bean.RegisterBean
import com.example.fragment.module.wan.bean.UserShareBean
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ShareModel : ViewModel() {

    val userShareResult = MutableLiveData<UserShareBean>()
    val shareArticleResult = MutableLiveData<HttpResponse>()

    var page = 0
    var pageCont = 1
    var isRefresh = true

    fun shareArticle(title: String, link: String) {
        viewModelScope.launch(Dispatchers.Main) {
            val request = HttpRequest("lg/user_article/add/json")
                .putParam("title", title)
                .putParam("link", link)
            val response = post<RegisterBean>(request)
            shareArticleResult.postValue(response)
        }
    }

    fun userShare(isRefresh: Boolean, id: String) {
        this.isRefresh = isRefresh
        viewModelScope.launch {
            if (isRefresh) {
                page = 1
                pageCont = 1
            } else {
                page++
            }
            if (page <= pageCont) {
                val request = HttpRequest("user/{id}/share_articles/{page}/json")
                request.putPath("id", id)
                request.putPath("page", page.toString())
                val response = get<UserShareBean>(request)
                response.data?.shareArticles?.pageCount?.let { pageCont = it.toInt() }
                userShareResult.postValue(response)
            }
        }
    }

}