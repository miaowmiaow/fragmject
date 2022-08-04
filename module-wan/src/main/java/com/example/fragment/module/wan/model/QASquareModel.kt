package com.example.fragment.module.wan.model

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.fragment.library.base.http.HttpRequest
import com.example.fragment.library.base.http.get
import com.example.fragment.library.base.model.BaseViewModel
import com.example.fragment.library.common.bean.ArticleListBean
import kotlinx.coroutines.launch

class QASquareModel : BaseViewModel() {

    private val userArticleResult: MutableLiveData<ArticleListBean> by lazy {
        MutableLiveData<ArticleListBean>().also {
            getUserArticleHome()
        }
    }

    fun userArticleResult(): LiveData<ArticleListBean> {
        return userArticleResult
    }

    fun clearUserArticleResult() {
        userArticleResult.value = null
    }

    fun getUserArticleHome() {
        getUserArticleList(getHomePage())
    }

    fun getUserArticleNext() {
        getUserArticleList(getNextPage())
    }

    /**
     * 获取广场列表
     * page 0开始
     */
    private fun getUserArticleList(page: Int) {
        //通过viewModelScope创建一个协程
        viewModelScope.launch {
            //构建请求体，传入请求参数
            val request = HttpRequest("user_article/list/{page}/json")
                .putPath("page", page.toString())
            //以get方式发起网络请求
            val response = get<ArticleListBean>(request) { updateProgress(it) }
            //如果LiveData.value == null，则在转场动画结束后加载数据，用于解决过度动画卡顿问题
            if (userArticleResult.value == null) {
                transitionAnimationEnd(request, response)
            }
            //根据接口返回更新总页码
            response.data?.pageCount?.let { updatePageCont(it.toInt()) }
            //通过LiveData通知界面更新
            userArticleResult.postValue(response)
        }
    }

}