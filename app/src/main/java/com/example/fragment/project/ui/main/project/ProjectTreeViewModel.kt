package com.example.fragment.project.ui.main.project

import androidx.lifecycle.viewModelScope
import com.example.fragment.project.bean.ProjectTreeBean
import com.example.fragment.project.bean.ProjectTreeListBean
import com.example.miaow.base.http.HttpRequest
import com.example.miaow.base.http.get
import com.example.miaow.base.vm.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProjectTreeUiState(
    var isLoading: Boolean = false,
    var result: MutableList<ProjectTreeBean> = ArrayList(),
)

class ProjectTreeViewModel : BaseViewModel() {

    private val _uiState = MutableStateFlow(ProjectTreeUiState())

    val uiState: StateFlow<ProjectTreeUiState> = _uiState

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
            val response = get<ProjectTreeListBean>(request)
            _uiState.update { state ->
                response.data?.let { data ->
                    state.result.clear()
                    state.result.addAll(data)
                }
                state.copy(isLoading = false)
            }
        }
    }

}