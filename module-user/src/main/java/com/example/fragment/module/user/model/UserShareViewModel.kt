package com.example.fragment.module.user.model

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.fragment.library.base.http.HttpRequest
import com.example.fragment.library.base.http.get
import com.example.fragment.library.base.model.BaseViewModel
import com.example.fragment.module.user.bean.UserShareBean
import kotlinx.coroutines.launch

class UserShareViewModel : BaseViewModel() {

    val userShareArticleResult = MutableLiveData<UserShareBean>()

    fun getUserShareArticles(id: String){
        getUserShareArticles(id, getHomePage(1))
    }

    fun getUserShareArticlesNext(id: String){
        getUserShareArticles(id, getNextPage())
    }

    private fun getUserShareArticles(id: String, page: Int) {
        viewModelScope.launch {
            val request = HttpRequest("user/{id}/share_articles/{page}/json")
            request.putPath("id", id)
            request.putPath("page", page.toString())
            val response = get<UserShareBean>(request) { progress(it) }
            response.data?.shareArticles?.pageCount?.let {updatePageCont(it.toInt())}
            userShareArticleResult.postValue(response)
        }
    }

}