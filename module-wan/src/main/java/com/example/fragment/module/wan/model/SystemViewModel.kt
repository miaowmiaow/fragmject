package com.example.fragment.module.wan.model

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.fragment.library.base.http.HttpRequest
import com.example.fragment.library.base.http.get
import com.example.fragment.library.base.model.BaseViewModel
import com.example.fragment.library.common.bean.ArticleBean
import com.example.fragment.library.common.bean.ArticleListBean
import com.example.fragment.library.common.bean.SystemTreeBean
import com.example.fragment.library.common.bean.SystemTreeListBean
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SystemViewModel : BaseViewModel() {

    val systemArticleResultMap: MutableMap<String, List<ArticleBean>> = HashMap()
    val systemArticleScrollMap: MutableMap<String, Int> = HashMap()

    private var cid: String = ""
    private val systemArticleResult = MutableLiveData<Map<String, ArticleListBean>>()

    fun systemArticleResult(cid: String): LiveData<Map<String, ArticleListBean>> {
        this.cid = cid
        if (systemArticleResultMap[cid].isNullOrEmpty()) {
            getSystemArticleHome(cid)
        }
        return systemArticleResult
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
            //如果LiveData.value == null，则在转场动画结束后加载数据，用于解决过度动画卡顿问题
            if (systemArticleResultMap[cid].isNullOrEmpty()) {
                delay(LOAD_DELAY_MILLIS)
            }
            //构建请求体，传入请求参数
            val request = HttpRequest("article/list/{page}/json")
                .putPath("page", page.toString())
                .putQuery("cid", cid)
            //以get方式发起网络请求
            val response = get<ArticleListBean>(request) { updateProgress(it) }
            //根据接口返回更新总页码
            response.data?.pageCount?.let { updatePageCont(it.toInt(), cid) }
            //通过LiveData通知界面更新
            systemArticleResult.postValue(mapOf(cid to response))
        }
    }

}