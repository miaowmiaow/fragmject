package com.example.fragment.module.wan.model

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.fragment.library.base.http.HttpRequest
import com.example.fragment.library.base.http.get
import com.example.fragment.library.base.model.BaseViewModel
import com.example.fragment.library.common.bean.ArticleListBean
import kotlinx.coroutines.launch

class QAModel : BaseViewModel() {

    val wendaResult = MutableLiveData<ArticleListBean>()
    val userArticleResult = MutableLiveData<ArticleListBean>()

    fun getWenDa() {
        getWenDaList(getHomePage(1))
    }

    fun getWenDaNext() {
        getWenDaList(getNextPage())
    }

    /**
     * 获取问答
     * page 1开始
     */
    private fun getWenDaList(page: Int) {
        //通过viewModelScope创建一个协程
        viewModelScope.launch {
            //构建请求体，传入请求参数
            val request = HttpRequest("wenda/list/{page}/json").putPath("page", page.toString())
            //以get方式发起网络请求
            val response = get<ArticleListBean>(request) { progress(it) }
            //根据接口返回更新总页码
            response.data?.pageCount?.let { updatePageCont(it.toInt()) }
            //通过LiveData通知界面更新
            wendaResult.postValue(response)
        }
    }

    fun getUserArticle() {
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
            val request =
                HttpRequest("user_article/list/{page}/json").putPath("page", page.toString())
            //以get方式发起网络请求
            val result = get<ArticleListBean>(request) { progress(it) }
            //根据接口返回更新总页码
            result.data?.pageCount?.let { updatePageCont(it.toInt()) }
            //通过LiveData通知界面更新
            userArticleResult.postValue(result)
        }
    }

}