package com.example.fragment.module.wan.model

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.fragment.library.base.http.HttpRequest
import com.example.fragment.library.base.http.get
import com.example.fragment.library.base.model.BaseViewModel
import com.example.fragment.library.common.bean.ArticleListBean
import kotlinx.coroutines.launch

class SystemViewModel : BaseViewModel() {

    val systemArticleResult = MutableLiveData<ArticleListBean>()

    fun getSystemArticle(cid: String) {
        getSystemList(cid, getHomePage())
    }

    fun getSystemArticleNext(cid: String) {
        getSystemList(cid, getNextPage())
    }

    /**
     * 获取知识体系下的文章
     * 	cid 分类id
     * 	page 0开始
     */
    private fun getSystemList(cid: String, page: Int) {
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