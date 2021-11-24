package com.example.fragment.module.wan.model

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.fragment.library.base.http.HttpRequest
import com.example.fragment.library.base.http.get
import com.example.fragment.library.base.model.BaseViewModel
import com.example.fragment.library.common.bean.ArticleListBean
import com.example.fragment.module.wan.bean.ProjectTreeBean
import kotlinx.coroutines.launch

class ProjectViewModel : BaseViewModel() {

    val projectTreeResult = MutableLiveData<ProjectTreeBean>()
    val projectListResult = MutableLiveData<ArticleListBean>()

    fun getProjectTree() {
        viewModelScope.launch {
            val request = HttpRequest("project/tree/json")
            val response = get<ProjectTreeBean>(request)
            projectTreeResult.postValue(response)
        }
    }

    fun getProject(cid: String){
        getProjectList(cid, getHomePage())
    }

    fun getProjectNext(cid: String){
        getProjectList(cid, getNextPage())
    }

    private fun getProjectList(cid: String, page: Int) {
        viewModelScope.launch {
            val request = HttpRequest("project/list/{page}/json")
            request.putQuery("cid", cid)
            request.putPath("page", page.toString())
            val response = get<ArticleListBean>(request) { progress(it) }
            response.data?.pageCount?.let {updatePageCont(it.toInt())}
            projectListResult.postValue(response)
        }
    }
}