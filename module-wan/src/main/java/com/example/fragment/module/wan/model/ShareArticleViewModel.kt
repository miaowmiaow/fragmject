package com.example.fragment.module.wan.model

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.fragment.library.base.http.HttpRequest
import com.example.fragment.library.base.http.get
import com.example.fragment.library.base.model.BaseViewModel
import com.example.fragment.module.wan.bean.ShareArticleBean
import kotlinx.coroutines.launch

class ShareArticleViewModel : BaseViewModel() {

    val userShareArticleResult = MutableLiveData<ShareArticleBean>()

    fun getUserShareArticles(id: String){
        getUserShareArticles(id, getHomePage(1))
    }

    fun getUserShareArticlesNext(id: String){
        getUserShareArticles(id, getNextPage())
    }

    /**
     * 获取用户分享文章
     * page 1开始
     */
    private fun getUserShareArticles(id: String, page: Int) {
        //通过viewModelScope创建一个协程
        viewModelScope.launch {
            //构建请求体，传入请求参数
            val request = HttpRequest("user/{id}/share_articles/{page}/json")
                .putPath("id", id)
                .putPath("page", page.toString())
            //以get方式发起网络请求
            val response = get<ShareArticleBean>(request) { updateProgress(it) }
            //根据接口返回更新总页码
            response.data?.shareArticles?.pageCount?.let {updatePageCont(it.toInt())}
            //通过LiveData通知界面更新
            userShareArticleResult.postValue(response)
        }
    }

}