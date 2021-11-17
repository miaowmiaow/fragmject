package com.example.fragment.module.home.model

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fragment.library.base.http.HttpRequest
import com.example.fragment.library.base.http.get
import com.example.fragment.library.common.bean.ArticleListBean
import com.example.fragment.library.common.utils.WanHelper
import com.example.fragment.library.common.bean.TreeListBean
import kotlinx.coroutines.launch

class SystemViewModel : ViewModel() {

    val treeResult = MutableLiveData<TreeListBean>()
    val treeListResult = MutableLiveData<ArticleListBean>()
    var page = 0
    var pageCont = 1
    var isRefresh = true

    fun getTree() {
        viewModelScope.launch {
            val request = HttpRequest("tree/json")
            val response = get<TreeListBean>(request)
            treeResult.postValue(response)
            response.data?.apply {
                WanHelper.setTreeList(this)
            }
        }
    }

    fun getTreeList(isRefresh: Boolean, cid: String) {
        this.isRefresh = isRefresh
        viewModelScope.launch {
            if (isRefresh) {
                page = 0
            } else {
                page++
            }
            if (page <= pageCont) {
                val request = HttpRequest("article/list/{page}/json")
                request.putPath("page", page.toString())
                request.putQuery("cid", cid)
                val response = get<ArticleListBean>(request)
                response.data?.pageCount?.let { pageCont = it.toInt() }
                treeListResult.postValue(response)
            }
        }
    }

}