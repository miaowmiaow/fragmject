package com.example.fragment.module.wan.model

import androidx.lifecycle.viewModelScope
import com.example.fragment.library.base.http.HttpRequest
import com.example.fragment.library.base.http.get
import com.example.fragment.library.common.bean.ArticleListBean
import kotlinx.coroutines.launch

class QAQuizModel : QAModel() {

    init {
        getHome()
    }

    override fun getHome() {
        refreshing = true
        geList(getHomePage(1))
    }

    override fun getNext() {
        loading = false
        geList(getNextPage())
    }

    /**
     * 获取问答
     * page 1开始
     */
    override fun geList(page: Int) {
        //通过viewModelScope创建一个协程
        viewModelScope.launch {
            //构建请求体，传入请求参数
            val request = HttpRequest("wenda/list/{page}/json").putPath("page", page.toString())
            //以get方式发起网络请求
            val response = get<ArticleListBean>(request)
            //根据接口返回更新总页码
            response.data?.pageCount?.let { updatePageCont(it.toInt()) }
            //如果LiveData.value == null，则在转场动画结束后加载数据，用于解决过度动画卡顿问题
            if (result.isEmpty()) {
                transitionAnimationEnd(request, response)
            }
            if (isHomePage()) {
                result.clear()
            }
            response.data?.datas?.let { result.addAll(it) }
            //设置下拉刷新状态
            refreshing = false
            //设置加载更多状态
            loading = hasNextPage()
        }
    }

}