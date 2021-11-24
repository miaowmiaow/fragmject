package com.example.fragment.module.user.model

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.fragment.library.base.http.HttpRequest
import com.example.fragment.library.base.http.get
import com.example.fragment.library.base.model.BaseViewModel
import com.example.fragment.library.common.bean.ArticleListBean
import kotlinx.coroutines.launch

class MyCollectViewModel : BaseViewModel() {

    val myCollectArticleResult = MutableLiveData<ArticleListBean>()

    fun getMyCollectArticle() {
        getMyCollectArticle(getHomePage())
    }

    fun getMyCollectArticleNext() {
        getMyCollectArticle(getNextPage())
    }

    private fun getMyCollectArticle(page: Int) {
        viewModelScope.launch {
            val request = HttpRequest("lg/collect/list/{page}/json")
            request.putPath("page", page.toString())
            val response = get<ArticleListBean>(request) { progress(it) }
            response.data?.pageCount?.let { updatePageCont(it.toInt()) }
            myCollectArticleResult.postValue(response)
        }
    }

}