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
import com.example.fragment.library.common.bean.BannerListBean
import com.example.fragment.library.common.bean.TopArticleBean
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

class HomeViewModel : BaseViewModel() {

    val result = ArrayList<ArticleBean>().toMutableStateList()

    var refreshing by mutableStateOf(false)
    var loading by mutableStateOf(true)

    init {
        getArticleHome()
    }

    fun getArticleHome() {
        refreshing = true
        //通过viewModelScope创建一个协程
        viewModelScope.launch {
            //通过async获取首页需要展示的数据
            val banner = async { getBanner() }
            val articleTop = async { getArticleTop() }
            val articleList = async { getArticleList(getHomePage()) }
            //通过LiveData通知界面更新
            val articleData: MutableList<ArticleBean> = arrayListOf()
            banner.await().data?.let { articleData.add(ArticleBean(banners = it, viewType = 0)) }
            articleTop.await().data?.onEach { it.top = true }?.let { articleData.addAll(it) }
            articleList.await().data?.datas?.let { articleData.addAll(it) }
            result.clear()
            result.addAll(articleData)
            //设置下拉刷新状态
            refreshing = false
            //设置加载更多状态
            loading = hasNextPage()
        }
    }

    fun getArticleNext() {
        loading = false
        viewModelScope.launch {
            getArticleList(getNextPage()).data?.datas?.let {
                result.addAll(it)
                //设置下拉刷新状态
                refreshing = false
                //设置加载更多状态
                loading = hasNextPage()
            }
        }
    }

    /**
     * 获取banner
     */
    private suspend fun getBanner(): BannerListBean {
        //构建请求体，传入请求参数
        val request = HttpRequest("banner/json")
        //以get方式发起网络请求
        return coroutineScope { get(request) }
    }

    /**
     * 获取置顶文章
     */
    private suspend fun getArticleTop(): TopArticleBean {
        //构建请求体，传入请求参数
        val request = HttpRequest("article/top/json")
        //以get方式发起网络请求
        return coroutineScope { get(request) }
    }

    /**
     * 获取首页文章列表
     * page 0开始
     */
    private suspend fun getArticleList(page: Int): ArticleListBean {
        //构建请求体，传入请求参数
        val request = HttpRequest("article/list/{page}/json").putPath("page", page.toString())
        //以get方式发起网络请求
        val response = coroutineScope { get<ArticleListBean>(request) { updateProgress(it) } }
        //如果LiveData.value == null，则在转场动画结束后加载数据，用于解决过度动画卡顿问题
        if (result.isEmpty()) {
            transitionAnimationEnd(request, response)
        }
        //根据接口返回更新总页码
        response.data?.pageCount?.let { updatePageCont(it.toInt()) }
        return response
    }

}