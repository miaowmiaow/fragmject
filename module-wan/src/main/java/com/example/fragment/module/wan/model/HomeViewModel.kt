package com.example.fragment.module.wan.model

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.fragment.library.base.http.HttpRequest
import com.example.fragment.library.base.http.get
import com.example.fragment.library.base.model.BaseViewModel
import com.example.fragment.library.common.bean.ArticleListBean
import com.example.fragment.library.common.bean.BannerListBean
import com.example.fragment.library.common.bean.TopArticleBean
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

class HomeViewModel : BaseViewModel() {

    val bannerResult = MutableLiveData<BannerListBean>()
    val articleTopResult = MutableLiveData<TopArticleBean>()
    val articleListResult = MutableLiveData<ArticleListBean>()

    fun getArticle() {
        //通过viewModelScope创建一个协程
        viewModelScope.launch {
            //设置请求进度，0.0请求开始
            progress(0.0)
            //通过async获取首页需要展示的数据
            val banner = async { getBanner() }
            val articleTop = async { getArticleTop() }
            val articleList = async { getArticleList(getHomePage()) }
            //通过LiveData通知界面更新
            bannerResult.postValue(banner.await())
            articleTopResult.postValue(articleTop.await())
            articleListResult.postValue(articleList.await())
            //设置请求进度，1.0请求结束
            progress(1.0)
        }
    }

    fun getArticleNext() {
        viewModelScope.launch {
            articleListResult.postValue(getArticleList(getNextPage()))
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
        val response = coroutineScope { get<ArticleListBean>(request) }
        //根据接口返回更新总页码
        response.data?.pageCount?.let { updatePageCont(it.toInt()) }
        return response
    }

}