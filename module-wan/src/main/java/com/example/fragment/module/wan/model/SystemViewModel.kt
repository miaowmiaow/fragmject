package com.example.fragment.module.wan.model

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.fragment.library.base.http.HttpRequest
import com.example.fragment.library.base.http.get
import com.example.fragment.library.base.model.BaseViewModel
import com.example.fragment.library.common.bean.ArticleListBean
import kotlinx.coroutines.launch

class SystemViewModel : BaseViewModel() {

    val systemArticleResult = MutableLiveData<ArticleListBean>()

    fun getSystemArticle(cid: String) {
        getSystemList(cid, getHomePage())
    }

    fun getSystemArticleNext(cid: String) {
        getSystemList(cid, getNextPage())
    }

    private fun getSystemList(cid: String, page: Int) {
        viewModelScope.launch {
            val request = HttpRequest("article/list/{page}/json")
            request.putQuery("cid", cid)
            request.putPath("page", page.toString())
            val response = get<ArticleListBean>(request) { progress(it) }
            response.data?.pageCount?.let { updatePageCont(it.toInt()) }
            systemArticleResult.postValue(response)
        }
    }

}