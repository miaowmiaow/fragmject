package com.example.fragmject.feature.wan.main.project

import androidx.lifecycle.viewModelScope
import com.example.fragmject.core.model.ProjectTree
import com.example.fragmject.core.data.repository.OfflineFirstProjectRepository
import com.example.fragmject.core.common.viewmodel.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ProjectTreeUiState {
    data class Success(
        val isLoading: Boolean = false,
        val result: List<ProjectTree> = emptyList(),
    ) : ProjectTreeUiState
}

val ProjectTreeUiState.isLoading get() = (this as? ProjectTreeUiState.Success)?.isLoading ?: false
val ProjectTreeUiState.result get() = (this as? ProjectTreeUiState.Success)?.result ?: emptyList()

private inline fun MutableStateFlow<ProjectTreeUiState>.updateData(
    crossinline block: (ProjectTreeUiState.Success) -> ProjectTreeUiState.Success,
) {
    update {
        val current = (it as? ProjectTreeUiState.Success) ?: ProjectTreeUiState.Success()
        block(current)
    }
}

@HiltViewModel
class ProjectTreeViewModel @Inject constructor(
    private val repo: OfflineFirstProjectRepository,
) : BaseViewModel() {

    private val _uiState = MutableStateFlow<ProjectTreeUiState>(ProjectTreeUiState.Success())
    val uiState: StateFlow<ProjectTreeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repo.observeProjectTree().collect { trees ->
                _uiState.updateData { it.copy(isLoading = false, result = trees) }
            }
        }
        viewModelScope.launch { repo.refreshProjectTree() }
    }
}
