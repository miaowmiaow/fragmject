package com.example.fragment.module.wan.model

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.fragment.library.base.http.HttpRequest
import com.example.fragment.library.base.http.get
import com.example.fragment.library.base.model.BaseViewModel
import com.example.fragment.library.common.bean.ArticleBean
import com.example.fragment.library.common.bean.ArticleListBean
import com.example.fragment.library.common.bean.ProjectTreeBean
import com.example.fragment.library.common.bean.ProjectTreeListBean
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ProjectTreeViewModel : BaseViewModel() {

    private val projectTreeResult: MutableLiveData<List<ProjectTreeBean>> by lazy {
        MutableLiveData<List<ProjectTreeBean>>().also {
            getProjectTree()
        }
    }

    fun projectTreeResult(): LiveData<List<ProjectTreeBean>> {
        return projectTreeResult
    }

    /**
     * 获取项目分类
     */
    private fun getProjectTree() {
        viewModelScope.launch {
            //如果LiveData.value == null，则在转场动画结束后加载数据，用于解决过度动画卡顿问题
            if (projectTreeResult.value == null) {
                delay(LOAD_DELAY_MILLIS)
            }
            val request = HttpRequest("project/tree/json")
            val response = get<ProjectTreeListBean>(request) { updateProgress(it) }
            response.data?.let {
                projectTreeResult.postValue(it)
            }
        }
    }

}