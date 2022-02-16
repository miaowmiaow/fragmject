package com.example.fragment.module.wan.model

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.fragment.library.base.http.HttpRequest
import com.example.fragment.library.base.http.get
import com.example.fragment.library.base.model.BaseViewModel
import com.example.fragment.library.common.bean.ArticleListBean
import com.example.fragment.library.common.bean.ProjectTreeBean
import com.example.fragment.library.common.bean.ProjectTreeListBean
import kotlinx.coroutines.launch

class ProjectViewModel : BaseViewModel() {

    val projectTreeResult = MutableLiveData<List<ProjectTreeBean>>()
    val projectListResult = MutableLiveData<ArticleListBean>()

    /**
     * 获取项目分类
     */
    fun getProjectTree(show: Boolean = false) {
        viewModelScope.launch {
            val request = HttpRequest("project/tree/json")
            val response = get<ProjectTreeListBean>(request) { if (show) progress(it) }
            response.data?.let {
                projectTreeResult.postValue(it)
            }
        }
    }

    fun getProject(cid: String) {
        getProjectList(cid, getHomePage(1))
    }

    fun getProjectNext(cid: String) {
        getProjectList(cid, getNextPage())
    }

    /**
     * 获取项目列表
     * cid 分类id
     * page 1开始
     */
    private fun getProjectList(cid: String, page: Int) {
        //通过viewModelScope创建一个协程
        viewModelScope.launch {
            //构建请求体，传入请求参数
            val request = HttpRequest("project/list/{page}/json")
                .putPath("page", page.toString())
                .putQuery("cid", cid)
            //以get方式发起网络请求
            val response = get<ArticleListBean>(request) { progress(it) }
            //根据接口返回更新总页码
            response.data?.pageCount?.let { updatePageCont(it.toInt()) }
            //通过LiveData通知界面更新
            projectListResult.postValue(response)
        }
    }
}