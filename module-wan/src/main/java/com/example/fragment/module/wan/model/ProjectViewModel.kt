package com.example.fragment.module.wan.model

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.fragment.library.base.http.HttpRequest
import com.example.fragment.library.base.http.get
import com.example.fragment.library.base.model.BaseViewModel
import com.example.fragment.library.common.bean.ArticleBean
import com.example.fragment.library.common.bean.ArticleListBean
import com.example.fragment.library.common.bean.ProjectTreeBean
import com.example.fragment.library.common.bean.ProjectTreeListBean
import kotlinx.coroutines.launch

class ProjectViewModel : BaseViewModel() {

    val treeResult = MutableLiveData<List<ProjectTreeBean>>()
    val listResult = MutableLiveData<Map<String, ArticleListBean>>()

    val listMap: MutableMap<String, List<ArticleBean>> = HashMap()
    val listScrollMap: MutableMap<String, Int> = HashMap()

    /**
     * 获取项目分类
     */
    fun getProjectTree() {
        viewModelScope.launch {
            val request = HttpRequest("project/tree/json")
            val response = get<ProjectTreeListBean>(request) { updateProgress(it) }
            response.data?.let {
                treeResult.postValue(it)
            }
        }
    }

    fun getProject(cid: String) {
        getProjectList(cid, getHomePage(1, cid))
    }

    fun getProjectNext(cid: String) {
        getProjectList(cid, getNextPage(cid))
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
            val response = get<ArticleListBean>(request)
            //根据接口返回更新总页码
            response.data?.pageCount?.let { updatePageCont(it.toInt(), cid) }
            //通过LiveData通知界面更新
            listResult.postValue(mapOf(cid to response))
        }
    }
}