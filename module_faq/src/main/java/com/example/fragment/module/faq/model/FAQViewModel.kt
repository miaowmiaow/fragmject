package com.example.fragment.module.faq.model

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.fragment.library.base.http.HttpRequest
import com.example.fragment.library.base.http.get
import com.example.fragment.library.common.bean.ArticleListBean
import com.example.fragment.library.common.model.BaseViewModel
import kotlinx.coroutines.launch

class FAQViewModel : BaseViewModel() {

    val wendaResult = MutableLiveData<ArticleListBean>()
    var page = 0
    var pageCont = 1
    var isRefresh = true

    fun getUserArticleList(isRefresh: Boolean) {
        this.isRefresh = isRefresh
        viewModelScope.launch {
            if (isRefresh) {
                page = 0
            } else {
                page++
            }
            if (page <= pageCont) {
                val request = HttpRequest("wenda/list/{page}/json")
                request.putPath("page", page.toString())
                val result = get<ArticleListBean>(request)
                result.data?.pageCount?.let { pageCont = it.toInt() }
                wendaResult.postValue(result)
            }
        }
    }

}