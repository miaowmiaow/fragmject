package com.example.fragment.module.wan.model

import androidx.lifecycle.viewModelScope
import com.example.fragment.library.base.http.HttpRequest
import com.example.fragment.library.base.http.get
import com.example.fragment.library.common.bean.ArticleListBean
import kotlinx.coroutines.launch

class QASquareModel : QAModel() {

    init {
        getHome()
    }

    override fun getHome() {
        refreshing = true
        geList(getHomePage())
    }

    override fun getNext() {
        loading = false
        geList(getNextPage())
    }

    /**
     * 获取广场列表
     * page 0开始
     */
    override fun geList(page: Int) {
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
            if (result.isEmpty()) {
                transitionAnimationEnd(request, response)
            }
            response.data?.datas?.let {
                if (isHomePage()) {
                    result.clear()
                }
                result.addAll(it)
            }
            //设置下拉刷新状态
            refreshing = false
            //设置加载更多状态
            loading = hasNextPage()
        }
    }

}