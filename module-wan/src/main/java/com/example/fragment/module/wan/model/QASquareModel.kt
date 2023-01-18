package com.example.fragment.module.wan.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.lifecycle.viewModelScope
import com.example.fragment.library.base.http.HttpRequest
import com.example.fragment.library.base.http.get
import com.example.fragment.library.base.model.BaseViewModel
import com.example.fragment.library.common.bean.ArticleBean
import com.example.fragment.library.common.bean.ArticleListBean
import kotlinx.coroutines.launch

class QASquareModel : BaseViewModel() {

    private val _userArticleResult = ArrayList<ArticleBean>()
    val userArticleResult: List<ArticleBean> get() = _userArticleResult.toMutableStateList()
    var refreshing by mutableStateOf(false)
    var loading by mutableStateOf(true)

    init {
        getUserArticleHome()
    }

    fun getUserArticleHome() {
        refreshing = true
        getUserArticleList(getHomePage())
    }

    fun getUserArticleNext() {
        loading = false
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
            //根据接口返回更新总页码
            response.data?.pageCount?.let { updatePageCont(it.toInt()) }
            if (userArticleResult.isEmpty()) {
                transitionAnimationEnd(request, response)
            }
            if (isHomePage()) {
                _userArticleResult.clear()
            }
            response.data?.datas?.let { _userArticleResult.addAll(it) }
            //设置下拉刷新状态
            refreshing = false
            //设置加载更多状态
            loading = hasNextPage()
        }
    }

}