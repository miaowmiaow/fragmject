package com.example.fragment.module.wan.model

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.fragment.library.base.http.HttpRequest
import com.example.fragment.library.base.http.get
import com.example.fragment.library.base.model.BaseViewModel
import com.example.fragment.library.common.bean.ArticleBean
import com.example.fragment.library.common.bean.ArticleListBean
import kotlinx.coroutines.launch

class SystemViewModel : BaseViewModel() {

    val listDataMap: MutableMap<String, List<ArticleBean>> = HashMap()
    val listScrollMap: MutableMap<String, Int> = HashMap()

    private var cid: String = ""
    private val systemArticleResult = MutableLiveData<Map<String, ArticleListBean>>()

    fun systemArticleResult(cid: String): LiveData<Map<String, ArticleListBean>> {
        this.cid = cid
        if (!listDataMap.containsKey(cid)) {
            getSystemArticleHome(cid)
        }
        return systemArticleResult
    }

    fun clearSystemArticleResult(cid: String) {
        systemArticleResult.value = mapOf(cid to ArticleListBean())
    }

    fun getSystemArticleHome(cid: String) {
        this.cid = cid
        getSystemArticleList(cid, getHomePage(key = cid))
    }

    fun getSystemArticleNext(cid: String) {
        this.cid = cid
        getSystemArticleList(cid, getNextPage(cid))
    }

    /**
     * 获取知识体系下的文章
     * 	cid 分类id
     * 	page 0开始
     */
    private fun getSystemArticleList(cid: String, page: Int) {
        //通过viewModelScope创建一个协程
        viewModelScope.launch {
            //构建请求体，传入请求参数
            val request = HttpRequest("article/list/{page}/json")
                .putPath("page", page.toString())
                .putQuery("cid", cid)
            //以get方式发起网络请求
            val response = get<ArticleListBean>(request) { updateProgress(it) }
            //如果LiveData.value == null，则在转场动画结束后加载数据，用于解决过度动画卡顿问题
            if (!listDataMap.containsKey(cid)) {
                transitionAnimationEnd(request, response)
            }
            //根据接口返回更新总页码
            response.data?.pageCount?.let { updatePageCont(it.toInt(), cid) }
            //通过LiveData通知界面更新
            systemArticleResult.postValue(mapOf(cid to response))
        }
    }

}