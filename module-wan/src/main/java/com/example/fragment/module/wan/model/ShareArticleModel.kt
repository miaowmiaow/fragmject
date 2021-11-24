package com.example.fragment.module.wan.model

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.fragment.library.base.http.HttpRequest
import com.example.fragment.library.base.http.HttpResponse
import com.example.fragment.library.base.http.post
import com.example.fragment.library.base.model.BaseViewModel
import com.example.fragment.library.common.bean.RegisterBean
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ShareArticleModel : BaseViewModel() {

    val shareArticleResult = MutableLiveData<HttpResponse>()

    fun getShareArticle(title: String, link: String) {
        viewModelScope.launch(Dispatchers.Main) {
            val request = HttpRequest("lg/user_article/add/json")
                .putParam("title", title)
                .putParam("link", link)
            val response = post<RegisterBean>(request) { progress(it) }
            shareArticleResult.postValue(response)
        }
    }

}