package com.example.fragment.project.ui.main.project

import androidx.lifecycle.viewModelScope
import com.example.fragment.project.data.Article
import com.example.fragment.project.data.repository.ProjectRepository
import com.example.fragment.project.data.repository.WanRepositoryProvider
import com.example.miaow.base.vm.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 不可变 UiState：通过整体替换 Map 引用来触发 StateFlow 的变化通知，
 * 不再依赖 updateTime = System.nanoTime() 这种 hack。
 */
data class ProjectUiState(
    val isRefreshing: Map<String, Boolean> = emptyMap(),
    val isLoading: Map<String, Boolean> = emptyMap(),
    val isFinishing: Map<String, Boolean> = emptyMap(),
    val result: Map<String, List<Article>> = emptyMap(),
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

    fun getResult(cid: String): List<Article>? {
        return result[cid]
    }

}

class ProjectViewModel(
    private val projectRepo: ProjectRepository = WanRepositoryProvider.project,
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(ProjectUiState())

    val uiState: StateFlow<ProjectUiState> = _uiState.asStateFlow()

    fun init(cid: String) {
        if (!uiState.value.result.containsKey(cid)) {
            getHome(cid)
        }
    }

    fun getHome(cid: String) {
        _uiState.update { state ->
            state.copy(
                isRefreshing = state.isRefreshing + (cid to true),
                isLoading = state.isLoading + (cid to false),
                isFinishing = state.isFinishing + (cid to false),
            )
        }
        getList(cid, getHomePage(1, cid))
    }

    fun getNext(cid: String) {
        _uiState.update { state ->
            state.copy(
                isRefreshing = state.isRefreshing + (cid to false),
                isLoading = state.isLoading + (cid to false),
                isFinishing = state.isFinishing + (cid to false),
            )
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
            val response = projectRepo.getProjectList(cid, page)
            updatePageCont(response.data?.pageCount?.toInt(), cid)
            _uiState.update { state ->
                val datas = response.data?.datas.orEmpty()
                val previous = state.result[cid].orEmpty()
                val merged = if (isHomePage(cid)) datas else previous + datas
                state.copy(
                    isRefreshing = state.isRefreshing + (cid to false),
                    isLoading = state.isLoading + (cid to hasNextPage(cid)),
                    isFinishing = state.isFinishing + (cid to !hasNextPage(cid)),
                    result = state.result + (cid to merged),
                )
            }
        }
    }

}