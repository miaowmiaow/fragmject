package com.example.fragmject.feature.home.ui.project

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fragmject.core.domain.repository.ProjectRepository
import com.example.fragmject.core.domain.result.DomainResult
import com.example.fragmject.core.model.ProjectTree
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ProjectTreeUiState {
    data object Loading : ProjectTreeUiState
    data class Success(
        val result: List<ProjectTree> = emptyList(),
    ) : ProjectTreeUiState
    data class Error(
        val code: String = "",
        val message: String = "",
    ) : ProjectTreeUiState
}

// Screen accessors
val ProjectTreeUiState.isLoading get() = this is ProjectTreeUiState.Loading
val ProjectTreeUiState.result get() = (this as? ProjectTreeUiState.Success)?.result ?: emptyList()
val ProjectTreeUiState.errorMessage get() = (this as? ProjectTreeUiState.Error)?.message ?: ""

@HiltViewModel
class ProjectTreeViewModel @Inject constructor(
    private val repo: ProjectRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProjectTreeUiState>(ProjectTreeUiState.Loading)
    val uiState: StateFlow<ProjectTreeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repo.observeProjectTree().collect { trees ->
                // observe 首发射（含本地缓存）后转 Success；后续数据更新同样转 Success
                _uiState.value = ProjectTreeUiState.Success(trees)
            }
        }
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = ProjectTreeUiState.Loading
            when (val r = repo.refreshProjectTree()) {
                is DomainResult.Success -> Unit
                is DomainResult.Failure ->
                    _uiState.value = ProjectTreeUiState.Error(r.code, r.message)
            }
        }
    }
}
