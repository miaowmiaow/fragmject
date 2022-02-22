package com.example.fragment.module.wan.model

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.fragment.library.base.http.HttpRequest
import com.example.fragment.library.base.http.get
import com.example.fragment.library.base.model.BaseViewModel
import com.example.fragment.library.common.bean.ArticleBean
import com.example.fragment.library.common.bean.ArticleListBean
import com.example.fragment.library.common.bean.SystemTreeBean
import com.example.fragment.library.common.bean.SystemTreeListBean
import kotlinx.coroutines.launch

class SystemViewModel : BaseViewModel() {

    val treeResult = MutableLiveData<List<SystemTreeBean>>()
    val listResult = MutableLiveData<Map<String, ArticleListBean>>()

    val listMap: MutableMap<String, List<ArticleBean>> = HashMap()
    val listScrollMap: MutableMap<String, Int> = HashMap()

    /**
     * 获取项目分类
     */
    fun getSystemTree() {
        viewModelScope.launch {
            val request = HttpRequest("tree/json")
            val response = get<SystemTreeListBean>(request) { updateProgress(it) }
            response.data?.let {
                treeResult.postValue(it)
            }
        }
    }

    fun getSystemArticle(cid: String) {
        getSystemArticleList(cid, getHomePage(key = cid))
    }

    fun getSystemArticleNext(cid: String) {
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
            val response = get<ArticleListBean>(request)
            //根据接口返回更新总页码
            response.data?.pageCount?.let { updatePageCont(it.toInt(), cid) }
            //通过LiveData通知界面更新
            listResult.postValue(mapOf(cid to response))
        }
    }

}