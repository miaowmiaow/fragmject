package com.example.fragment.module.home.model

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.fragment.library.base.http.HttpRequest
import com.example.fragment.library.base.http.get
import com.example.fragment.library.common.model.BaseViewModel
import com.example.fragment.library.common.bean.ArticleBean
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SquareViewModel : BaseViewModel() {

    val userArticleResult = MutableLiveData<ArticleBean>()
    var page = 0
    var pageCont = 1
    var isRefresh = true

    fun getUserArticleList(isRefresh: Boolean) {
        this.isRefresh = isRefresh
        viewModelScope.launch(Dispatchers.Main) {
            if (isRefresh) {
                page = 0
            } else {
                page++
            }
            if (page <= pageCont) {
                val request = HttpRequest("user_article/list/{page}/json")
                request.putPath("page", page.toString())
                val result = get<ArticleBean>(request)
                result.data?.pageCount?.let { pageCont = it.toInt() }
                userArticleResult.postValue(result)
            }
        }
    }

}