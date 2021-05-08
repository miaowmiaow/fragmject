package com.example.fragment.module.system.model

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.fragment.library.base.http.HttpRequest
import com.example.fragment.library.base.http.get
import com.example.fragment.library.common.bean.ArticleListBean
import com.example.fragment.library.common.model.BaseViewModel
import com.example.fragment.module.system.bean.TreeListBean
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SystemViewModel : BaseViewModel(){

    val treeResult = MutableLiveData<TreeListBean>()
    val treeListResult = MutableLiveData<ArticleListBean>()
    var page = 0
    var pageCont = 1
    var isRefresh = true

    fun getTree() {
        viewModelScope.launch(Dispatchers.Main) {
            val request = HttpRequest("tree/json")
            treeResult.postValue(get(request))
        }
    }

    fun getTreeList(isRefresh: Boolean, cid: String) {
        this.isRefresh = isRefresh
        viewModelScope.launch(Dispatchers.Main) {
            if (isRefresh) {
                page = 0
            } else {
                page++
            }
            if (page <= pageCont) {
                val request = HttpRequest("article/list/{page}/json")
                request.putPath("page", page.toString())
                request.putQuery("cid", cid)
                val result = get<ArticleListBean>(request)
                result.data?.pageCount?.let { pageCont = it.toInt() }
                treeListResult.postValue(result)
            }
        }
    }

}