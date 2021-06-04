package com.example.fragment.user.model

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.fragment.library.base.http.HttpRequest
import com.example.fragment.library.base.http.HttpResponse
import com.example.fragment.library.base.http.get
import com.example.fragment.library.base.http.post
import com.example.fragment.library.common.bean.*
import com.example.fragment.library.common.model.BaseViewModel
import com.example.fragment.user.bean.UserShareBean
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class UserViewModel : BaseViewModel() {

    val loginResult = MutableLiveData<LoginBean>()
    val registerResult = MutableLiveData<RegisterBean>()
    val logoutResult = MutableLiveData<HttpResponse>()
    val shareArticleResult = MutableLiveData<HttpResponse>()
    val userCoinResult = MutableLiveData<UserCoinBean>()
    val myCoinResult = MutableLiveData<MyCoinListBean>()
    val coinRankResult = MutableLiveData<CoinRankBean>()
    val myCollectArticleResult = MutableLiveData<ArticleListBean>()
    val myShareArticleResult = MutableLiveData<ShareArticleListBean>()
    val userShareResult = MutableLiveData<UserShareBean>()

    var page = 0
    var pageCont = 1
    var isRefresh = true

    fun login(username: String, password: String) {
        viewModelScope.launch {
            val request = HttpRequest("user/login")
                .putParam("username", username)
                .putParam("password", password)
            val response = post<LoginBean>(request)
            loginResult.postValue(response)
        }
    }

    fun register(username: String, password: String, repassword: String) {
        viewModelScope.launch {
            val request = HttpRequest("user/register")
                .putParam("username", username)
                .putParam("password", password)
                .putParam("repassword", repassword)
            val response = post<RegisterBean>(request)
            registerResult.postValue(response)
        }
    }

    fun logout() {
        viewModelScope.launch {
            val request = HttpRequest("user/logout/json")
            val response = get<HttpResponse>(request)
            logoutResult.postValue(response)
        }
    }

    fun shareArticle(title: String, link: String) {
        viewModelScope.launch(Dispatchers.Main) {
            val request = HttpRequest("lg/user_article/add/json")
                .putParam("title", title)
                .putParam("link", link)
            val response = post<RegisterBean>(request)
            registerResult.postValue(response)
        }
    }

    fun userCoin() {
        viewModelScope.launch {
            val request = HttpRequest("lg/coin/userinfo/jsonn")
            val response = get<UserCoinBean>(request)
            userCoinResult.postValue(response)
        }
    }

    fun myCoin(isRefresh: Boolean) {
        this.isRefresh = isRefresh
        viewModelScope.launch {
            if (isRefresh) {
                page = 1
                pageCont = 1
            } else {
                page++
            }
            if (page <= pageCont) {
                val request = HttpRequest("lg/coin/list/{page}/json")
                request.putPath("page", page.toString())
                val response = get<MyCoinListBean>(request)
                response.data?.pageCount?.let { pageCont = it.toInt() }
                myCoinResult.postValue(response)
            }
        }
    }

    fun coinRank(isRefresh: Boolean) {
        this.isRefresh = isRefresh
        viewModelScope.launch {
            if (isRefresh) {
                page = 1
                pageCont = 1
            } else {
                page++
            }
            if (page <= pageCont) {
                val request = HttpRequest("coin/rank/{page}/json")
                request.putPath("page", page.toString())
                val response = get<CoinRankBean>(request)
                response.data?.pageCount?.let { pageCont = it.toInt() }
                coinRankResult.postValue(response)
            }
        }
    }

    fun myCollectArticle(isRefresh: Boolean) {
        this.isRefresh = isRefresh
        viewModelScope.launch {
            if (isRefresh) page = 0 else page++
            if (page <= pageCont) {
                val request = HttpRequest("lg/collect/list/{page}/json")
                request.putPath("page", page.toString())
                val response = get<ArticleListBean>(request)
                response.data?.pageCount?.let { pageCont = it.toInt() }
                myCollectArticleResult.postValue(response)
            }
        }
    }

    fun myShareArticle(isRefresh: Boolean) {
        this.isRefresh = isRefresh
        viewModelScope.launch {
            if (isRefresh) {
                page = 1
                pageCont = 1
            } else {
                page++
            }
            if (page <= pageCont) {
                val request = HttpRequest("user/lg/private_articles/{page}/json")
                request.putPath("page", page.toString())
                val response = get<ShareArticleListBean>(request)
                response.data?.shareArticles?.pageCount?.let { pageCont = it.toInt() }
                myShareArticleResult.postValue(response)
            }
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