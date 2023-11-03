package com.example.fragment.project.ui.main.project

import androidx.lifecycle.viewModelScope
import com.example.fragment.project.bean.ArticleBean
import com.example.fragment.project.bean.ArticleListBean
import com.example.miaow.base.http.get
import com.example.miaow.base.vm.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProjectUiState(
    val refreshing: MutableMap<String, Boolean> = HashMap(),
    val loading: MutableMap<String, Boolean> = HashMap(),
    val finishing: MutableMap<String, Boolean> = HashMap(),
    val result: MutableMap<String, ArrayList<ArticleBean>> = HashMap(),
    val updateTime: Long = 0
) {
    fun getRefreshing(cid: String): Boolean {
        return refreshing[cid] ?: true
    }

    fun getLoading(cid: String): Boolean {
        return loading[cid] ?: false
    }

    fun getFinishing(cid: String): Boolean {
        return finishing[cid] ?: false
    }

    fun getResult(cid: String): ArrayList<ArticleBean>? {
        return result[cid]
    }

}

class ProjectViewModel : BaseViewModel() {

    private val _uiState = MutableStateFlow(ProjectUiState())

    val uiState: StateFlow<ProjectUiState> = _uiState.asStateFlow()

    fun init(cid: String) {
        if (!uiState.value.result.containsKey(cid)) {
            getHome(cid)
        }
    }

    fun getHome(cid: String) {
        _uiState.update { state ->
            state.refreshing[cid] = true
            state.loading[cid] = false
            state.finishing[cid] = false
            state.copy(updateTime = System.nanoTime())
        }
        getList(cid, getHomePage(1, cid))
    }

    fun getNext(cid: String) {
        _uiState.update { state ->
            state.refreshing[cid] = false
            state.loading[cid] = false
            state.finishing[cid] = false
            state.copy(updateTime = System.nanoTime())
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
            val response = get<ArticleListBean> {
                setUrl("project/list/{page}/json")
                putPath("page", page.toString())
                putQuery("cid", cid)
            }
            updatePageCont(response.data?.pageCount?.toInt(), cid)
            response.data?.let { data ->
                _uiState.update { state ->
                    data.datas?.let { datas ->
                        if (isHomePage(cid)) {
                            state.result[cid] = arrayListOf()
                        }
                        state.result[cid]?.addAll(datas)
                    }
                    state.refreshing[cid] = false
                    state.loading[cid] = hasNextPage(cid)
                    state.finishing[cid] = !hasNextPage(cid)
                    state.copy(updateTime = System.nanoTime())
                }
            }
        }
    }

}