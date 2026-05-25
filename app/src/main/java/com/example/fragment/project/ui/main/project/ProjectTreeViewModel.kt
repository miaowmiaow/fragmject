package com.example.fragment.project.ui.main.project

import androidx.lifecycle.viewModelScope
import com.example.fragment.project.data.ProjectTree
import com.example.fragment.project.data.repository.ProjectRepository
import com.example.fragment.project.data.repository.WanRepositoryProvider
import com.example.miaow.base.vm.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProjectTreeUiState(
    val isLoading: Boolean = false,
    val result: List<ProjectTree> = emptyList(),
)

class ProjectTreeViewModel(
    private val projectRepo: ProjectRepository = WanRepositoryProvider.project,
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(ProjectTreeUiState())

    val uiState: StateFlow<ProjectTreeUiState> = _uiState.asStateFlow()

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
            val response = projectRepo.getProjectTree()
            _uiState.update { state ->
                state.copy(
                    isLoading = false,
                    result = response.data ?: state.result,
                )
            }
        }
    }

}