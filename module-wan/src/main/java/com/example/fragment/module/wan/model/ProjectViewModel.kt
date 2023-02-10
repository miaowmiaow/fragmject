package com.example.fragment.module.wan.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.fragment.library.base.http.HttpRequest
import com.example.fragment.library.base.http.get
import com.example.fragment.library.base.model.BaseViewModel
import com.example.fragment.library.common.bean.ArticleBean
import com.example.fragment.library.common.bean.ArticleListBean
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProjectState(
    val refreshing: MutableMap<String, Boolean> = HashMap(),
    val loading: MutableMap<String, Boolean> = HashMap(),
    val result: MutableMap<String, ArrayList<ArticleBean>> = HashMap(),
    var time: Long = 0
) {
    fun getRefreshing(cid: String): Boolean {
        return refreshing[cid] ?: false
    }

    fun getLoading(cid: String): Boolean {
        return loading[cid] ?: false
    }

    fun getResult(cid: String): ArrayList<ArticleBean>? {
        return result[cid]
    }

}

class ProjectViewModel(private val initialPage: Int) : BaseViewModel() {

    private val _uiState = MutableStateFlow(ProjectState(time = 0))

    val uiState: StateFlow<ProjectState> = _uiState

    fun init(cid: String) {
        if (!uiState.value.result.containsKey(cid)) {
            getHome(cid)
        }
    }

    fun getHome(cid: String) {
        _uiState.update {
            it.refreshing[cid] = true
            it.copy(time = System.currentTimeMillis())
        }
        getList(cid, getHomePage(initialPage, cid))
    }

    fun getNext(cid: String) {
        _uiState.update {
            it.loading[cid] = false
            it.copy(time = System.currentTimeMillis())
        }
        getList(cid, getNextPage(cid))
    }

    /**
     * 获取项目列表
     * cid 分类id
     * page 1开始
     */
    private fun getList(cid: String, page: Int) {
        viewModelScope.launch {
            val request = HttpRequest("project/list/{page}/json")
                .putPath("page", page.toString())
                .putQuery("cid", cid)
            val response = get<ArticleListBean>(request) { updateProgress(it) }
            //根据接口返回更新总页码
            response.data?.pageCount?.let { updatePageCont(it.toInt(), cid) }
            _uiState.update {
                response.data?.datas?.let { datas ->
                    if (isHomePage(cid)) {
                        it.result[cid] = arrayListOf()
                    }
                    it.result[cid]?.addAll(datas)
                }
                //设置下拉刷新状态
                it.refreshing[cid] = false
                //设置加载更多状态
                it.loading[cid] = hasNextPage()
                it.copy(time = System.currentTimeMillis())
            }
        }
    }

    companion object {
        fun provideFactory(
            initialPage: Int,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ProjectViewModel(initialPage) as T
            }
        }
    }
}