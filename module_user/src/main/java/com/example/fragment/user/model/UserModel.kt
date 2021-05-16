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

class UserModel : BaseViewModel() {

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
        viewModelScope.launch(Dispatchers.Main) {
            loginResult.postValue(
                post(
                    HttpRequest("user/login")
                        .putParam("username", username)
                        .putParam("password", password)
                )
            )
        }
    }

    fun register(username: String, password: String, repassword: String) {
        viewModelScope.launch(Dispatchers.Main) {
            registerResult.postValue(
                post(
                    HttpRequest("user/register")
                        .putParam("username", username)
                        .putParam("password", password)
                        .putParam("repassword", repassword)
                )
            )
        }
    }

    fun logout() {
        viewModelScope.launch(Dispatchers.Main) {
            logoutResult.postValue(get(HttpRequest("user/logout/json")))
        }
    }

    fun shareArticle(title: String, link: String) {
        viewModelScope.launch(Dispatchers.Main) {
            registerResult.postValue(
                post(
                    HttpRequest("lg/user_article/add/json")
                        .putParam("title", title)
                        .putParam("link", link)
                )
            )
        }
    }

    fun userCoin() {
        viewModelScope.launch(Dispatchers.Main) {
            userCoinResult.postValue(get(HttpRequest("lg/coin/userinfo/json")))
        }
    }

    fun myCoin(isRefresh: Boolean) {
        this.isRefresh = isRefresh
        viewModelScope.launch(Dispatchers.Main) {
            if (isRefresh) {
                page = 1
            } else {
                page++
            }
            if (page <= pageCont) {
                val request = HttpRequest("lg/coin/list/{page}/json")
                request.putPath("page", page.toString())
                val result = get<MyCoinListBean>(request)
                result.data?.pageCount?.let { pageCont = it.toInt() }
                myCoinResult.postValue(result)
            }
        }
    }

    fun coinRank(isRefresh: Boolean) {
        this.isRefresh = isRefresh
        viewModelScope.launch(Dispatchers.Main) {
            if (isRefresh) {
                page = 1
            } else {
                page++
            }
            if (page <= pageCont) {
                val request = HttpRequest("coin/rank/{page}/json")
                request.putPath("page", page.toString())
                val result = get<CoinRankBean>(request)
                result.data?.pageCount?.let { pageCont = it.toInt() }
                coinRankResult.postValue(result)
            }
        }
    }

    fun myCollectArticle(isRefresh: Boolean) {
        this.isRefresh = isRefresh
        viewModelScope.launch(Dispatchers.Main) {
            if (isRefresh) {
                page = 0
            } else {
                page++
            }
            if (page <= pageCont) {
                val request = HttpRequest("lg/collect/list/{page}/json")
                request.putPath("page", page.toString())
                val result = get<ArticleListBean>(request)
                result.data?.pageCount?.let { pageCont = it.toInt() }
                myCollectArticleResult.postValue(result)
            }
        }
    }

    fun myShareArticle(isRefresh: Boolean) {
        this.isRefresh = isRefresh
        viewModelScope.launch(Dispatchers.Main) {
            if (isRefresh) {
                page = 1
            } else {
                page++
            }
            if (page <= pageCont) {
                val request = HttpRequest("user/lg/private_articles/{page}/json")
                request.putPath("page", page.toString())
                val result = get<ShareArticleListBean>(request)
                result.data?.shareArticles?.pageCount?.let { pageCont = it.toInt() }
                myShareArticleResult.postValue(result)
            }
        }
    }

    fun userShare(isRefresh: Boolean, id: String) {
        this.isRefresh = isRefresh
        viewModelScope.launch(Dispatchers.Main) {
            if (isRefresh) {
                page = 1
            } else {
                page++
            }
            if (page <= pageCont) {
                val request = HttpRequest("user/{id}/share_articles/{page}/json")
                request.putPath("id", id)
                request.putPath("page", page.toString())
                val result = get<UserShareBean>(request)
                result.data?.shareArticles?.pageCount?.let { pageCont = it.toInt() }
                userShareResult.postValue(result)
            }
        }
    }

}