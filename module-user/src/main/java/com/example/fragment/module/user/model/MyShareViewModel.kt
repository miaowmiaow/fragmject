package com.example.fragment.module.user.model

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.fragment.library.base.http.HttpRequest
import com.example.fragment.library.base.http.get
import com.example.fragment.library.base.model.BaseViewModel
import com.example.fragment.library.common.bean.ShareArticleListBean
import kotlinx.coroutines.launch

class MyShareViewModel : BaseViewModel() {

    private val myShareArticleResult: MutableLiveData<ShareArticleListBean> by lazy {
        MutableLiveData<ShareArticleListBean>().also {
            getMyShareArticleHome()
        }
    }

    fun myShareArticleResult(): LiveData<ShareArticleListBean> {
        return myShareArticleResult
    }

    fun getMyShareArticleHome() {
        getMyShareArticle(getHomePage(1))
    }

    fun getMyShareArticleNext() {
        getMyShareArticle(getNextPage())
    }

    /**
     * 获取自己的分享的文章
     * page 1开始
     */
    private fun getMyShareArticle(page: Int) {
        //通过viewModelScope创建一个协程
        viewModelScope.launch {
            //构建请求体，传入请求参数
            val request = HttpRequest("user/lg/private_articles/{page}/json")
                .putPath("page", page.toString())
            //以get方式发起网络请求
            val response = get<ShareArticleListBean>(request) { updateProgress(it) }
            //如果LiveData.value == null，则在转场动画结束后加载数据，用于解决过度动画卡顿问题
            if (myShareArticleResult.value == null) {
                transitionAnimationEnd(request, response)
            }
            //根据接口返回更新总页码
            response.data?.shareArticles?.pageCount?.let { updatePageCont(it.toInt()) }
            //通过LiveData通知界面更新
            myShareArticleResult.postValue(response)
        }
    }

}