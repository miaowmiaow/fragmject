package com.example.fragment.module.project.model

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.fragment.library.base.http.HttpRequest
import com.example.fragment.library.base.http.get
import com.example.fragment.library.common.model.BaseViewModel
import com.example.fragment.module.project.bean.ProjectListBean
import com.example.fragment.module.project.bean.ProjectTreeBean
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ProjectViewModel : BaseViewModel() {

    val projectTreeResult = MutableLiveData<ProjectTreeBean>()
    val projectListResult = MutableLiveData<ProjectListBean>()
    var page = 0
    var pageCont = 1
    var isRefresh = true

    fun getProjectTree(isRefresh: Boolean) {
        viewModelScope.launch(Dispatchers.Main) {
            val request = HttpRequest("project/tree/json")
            projectTreeResult.postValue(get(request))
        }
    }

    fun getProjectList(isRefresh: Boolean, cid: String) {
        this.isRefresh = isRefresh
        viewModelScope.launch(Dispatchers.Main) {
            if (isRefresh) {
                page = 0
            } else {
                page++
            }
            if (page <= pageCont) {
                val request = HttpRequest("project/list/{page}/json")
                request.putPath("page", page.toString())
                request.putQuery("cid", cid)
                projectListResult.postValue(get(request))
            }
        }
    }
}