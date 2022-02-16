package com.example.fragment.module.wan.model

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.fragment.library.base.http.HttpRequest
import com.example.fragment.library.base.http.get
import com.example.fragment.library.base.model.BaseViewModel
import com.example.fragment.library.common.bean.ArticleListBean
import com.example.fragment.library.common.bean.SystemTreeBean
import com.example.fragment.library.common.bean.SystemTreeListBean
import kotlinx.coroutines.launch

class SystemViewModel : BaseViewModel() {

    val systemTreeResult = MutableLiveData<List<SystemTreeBean>>()
    val systemArticleResult = MutableLiveData<ArticleListBean>()

    /**
     * 获取项目分类
     */
    fun getSystemTree(show: Boolean = false) {
        viewModelScope.launch {
            val request = HttpRequest("tree/json")
            val response = get<SystemTreeListBean>(request) { if (show) progress(it) }
            response.data?.let {
                systemTreeResult.postValue(it)
            }
        }
    }

    fun getSystemArticle(cid: String) {
        getSystemArticleList(cid, getHomePage())
    }

    fun getSystemArticleNext(cid: String) {
        getSystemArticleList(cid, getNextPage())
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
            val response = get<ArticleListBean>(request) { progress(it) }
            //根据接口返回更新总页码
            response.data?.pageCount?.let { updatePageCont(it.toInt()) }
            //通过LiveData通知界面更新
            systemArticleResult.postValue(response)
        }
    }

}