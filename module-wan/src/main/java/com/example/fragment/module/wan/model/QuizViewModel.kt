package com.example.fragment.module.wan.model

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.fragment.library.base.http.HttpRequest
import com.example.fragment.library.base.http.get
import com.example.fragment.library.base.model.BaseViewModel
import com.example.fragment.library.common.bean.ArticleListBean
import kotlinx.coroutines.launch

class QuizViewModel : BaseViewModel() {

    val wendaResult = MutableLiveData<ArticleListBean>()

    fun getWenDa() {
        getWenDaList(getHomePage())
    }

    fun getWenDaNext() {
        getWenDaList(getNextPage())
    }

    private fun getWenDaList(page: Int) {
        viewModelScope.launch {
            val request = HttpRequest("wenda/list/{page}/json")
            request.putPath("page", page.toString())
            val response = get<ArticleListBean>(request) { progress(it) }
            response.data?.pageCount?.let { updatePageCont(it.toInt()) }
            wendaResult.postValue(response)
        }
    }

}