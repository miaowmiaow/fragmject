package com.example.fragment.project.ui.main.project

import androidx.lifecycle.viewModelScope
import com.example.fragment.library.base.http.HttpRequest
import com.example.fragment.library.base.http.get
import com.example.fragment.library.base.vm.BaseViewModel
import com.example.fragment.project.bean.ProjectTreeBean
import com.example.fragment.project.bean.ProjectTreeListBean
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProjectTreeState(
    var isLoading: Boolean = false,
    var result: MutableList<ProjectTreeBean> = ArrayList(),
)

class ProjectTreeViewModel : BaseViewModel() {

    private val _uiState = MutableStateFlow(ProjectTreeState())

    val uiState: StateFlow<ProjectTreeState> = _uiState

    init {
        getProjectTree()
    }

    /**
     * 获取项目分类
     */
    private fun getProjectTree() {
        _uiState.update {
            it.copy(isLoading = true)
        }
        viewModelScope.launch {
            val request = HttpRequest("project/tree/json")
            val response = get<ProjectTreeListBean>(request) { updateProgress(it) }
            _uiState.update {
                response.data?.let { data ->
                    it.result.clear()
                    it.result.addAll(data)
                }
                it.copy(isLoading = false)
            }
        }
    }

}