package com.example.fragment.project.ui.main.project

import androidx.lifecycle.viewModelScope
import com.example.fragment.project.data.ProjectTree
import com.example.fragment.project.data.ProjectTreeList
import com.example.miaow.base.http.get
import com.example.miaow.base.vm.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProjectTreeUiState(
    var isLoading: Boolean = false,
    var result: List<ProjectTree> = ArrayList(),
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
            val response = get<ProjectTreeList> {
                setUrl("project/tree/json")
            }
            _uiState.update { state ->
                response.data?.let { data ->
                    state.result = data
                }
                state.copy(isLoading = false)
            }
        }
    }

}