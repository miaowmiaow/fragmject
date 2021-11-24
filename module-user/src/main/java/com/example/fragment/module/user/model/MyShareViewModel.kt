package com.example.fragment.module.user.model

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.fragment.library.base.http.HttpRequest
import com.example.fragment.library.base.http.HttpResponse
import com.example.fragment.library.base.http.get
import com.example.fragment.library.base.http.post
import com.example.fragment.library.base.model.BaseViewModel
import com.example.fragment.library.common.bean.*
import kotlinx.coroutines.launch

class MyShareViewModel : BaseViewModel() {

    val myShareArticleResult = MutableLiveData<ShareArticleListBean>()

    fun getMyShareArticle(){
        getMyShareArticle(getHomePage(1))
    }

    fun getMyShareArticleNext(){
        getMyShareArticle(getNextPage())
    }

    private fun getMyShareArticle(page: Int) {
        viewModelScope.launch {
            val request = HttpRequest("user/lg/private_articles/{page}/json")
            request.putPath("page", page.toString())
            val response = get<ShareArticleListBean>(request) { progress(it) }
            response.data?.shareArticles?.pageCount?.let {updatePageCont(it.toInt())}
            myShareArticleResult.postValue(response)
        }
    }

}