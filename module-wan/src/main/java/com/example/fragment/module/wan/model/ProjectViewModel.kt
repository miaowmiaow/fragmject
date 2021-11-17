package com.example.fragment.module.wan.model

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fragment.library.base.http.HttpRequest
import com.example.fragment.library.base.http.get
import com.example.fragment.library.common.bean.ArticleListBean
import com.example.fragment.module.wan.bean.ProjectTreeBean
import kotlinx.coroutines.launch

class ProjectViewModel : ViewModel() {

    val projectTreeResult = MutableLiveData<ProjectTreeBean>()
    val projectListResult = MutableLiveData<ArticleListBean>()
    var page = 0
    var pageCont = 1
    var isRefresh = true

    fun getProjectTree() {
        viewModelScope.launch {
            val request = HttpRequest("project/tree/json")
            val response = get<ProjectTreeBean>(request)
            projectTreeResult.postValue(response)
        }
    }

    fun getProjectList(isRefresh: Boolean, cid: String) {
        this.isRefresh = isRefresh
        viewModelScope.launch {
            if (isRefresh) page = 0 else page++
            if (page <= pageCont) {
                val request = HttpRequest("project/list/{page}/json")
                request.putPath("page", page.toString())
                request.putQuery("cid", cid)
                val response = get<ArticleListBean>(request)
                response.data?.pageCount?.let { pageCont = it.toInt() }
                projectListResult.postValue(response)
            }
        }
    }
}