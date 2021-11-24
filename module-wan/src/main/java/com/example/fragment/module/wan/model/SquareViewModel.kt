package com.example.fragment.module.wan.model

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.fragment.library.base.http.HttpRequest
import com.example.fragment.library.base.http.get
import com.example.fragment.library.base.model.BaseViewModel
import com.example.fragment.library.common.bean.ArticleListBean
import kotlinx.coroutines.launch

class SquareViewModel : BaseViewModel() {

    val userArticleResult = MutableLiveData<ArticleListBean>()

    fun getUserArticle() {
        getUserArticleList(getHomePage())
    }

    fun getUserArticleNext() {
        getUserArticleList(getNextPage())
    }

    private fun getUserArticleList(page: Int) {
        viewModelScope.launch {
            val request = HttpRequest("user_article/list/{page}/json")
            request.putPath("page", page.toString())
            val result = get<ArticleListBean>(request) { progress(it) }
            result.data?.pageCount?.let { updatePageCont(it.toInt()) }
            userArticleResult.postValue(result)
        }
    }

}