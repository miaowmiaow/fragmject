package com.example.fragment.project.ui.main.project

import androidx.lifecycle.viewModelScope
import com.example.fragment.project.data.Article
import com.example.fragment.project.data.ArticleList
import com.example.miaow.base.http.get
import com.example.miaow.base.vm.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProjectUiState(
    val isRefreshing: MutableMap<String, Boolean> = HashMap(),
    val isLoading: MutableMap<String, Boolean> = HashMap(),
    val isFinishing: MutableMap<String, Boolean> = HashMap(),
    val result: MutableMap<String, ArrayList<Article>> = HashMap(),
    val updateTime: Long = 0
) {
    fun getRefreshing(cid: String): Boolean {
        return isRefreshing[cid] ?: true
    }

    fun getLoading(cid: String): Boolean {
        return isLoading[cid] ?: false
    }

    fun getFinishing(cid: String): Boolean {
        return isFinishing[cid] ?: false
    }

    fun getResult(cid: String): ArrayList<Article>? {
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
            state.isRefreshing[cid] = true
            state.isLoading[cid] = false
            state.isFinishing[cid] = false
            state.copy(updateTime = System.nanoTime())
        }
        getList(cid, getHomePage(1, cid))
    }

    fun getNext(cid: String) {
        _uiState.update { state ->
            state.isRefreshing[cid] = false
            state.isLoading[cid] = false
            state.isFinishing[cid] = false
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
            val response = get<ArticleList> {
                setUrl("project/list/{page}/json")
                putPath("page", page.toString())
                putQuery("cid", cid)
            }
            updatePageCont(response.data?.pageCount?.toInt(), cid)
            _uiState.update { state ->
                response.data?.datas?.let { datas ->
                    if (isHomePage(cid)) {
                        state.result[cid] = arrayListOf()
                    }
                    state.result[cid]?.addAll(datas)
                }
                state.isRefreshing[cid] = false
                state.isLoading[cid] = hasNextPage(cid)
                state.isFinishing[cid] = !hasNextPage(cid)
                state.copy(updateTime = System.nanoTime())
            }
        }
    }

}