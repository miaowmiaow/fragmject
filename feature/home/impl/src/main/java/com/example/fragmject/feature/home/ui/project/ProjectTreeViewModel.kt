package com.example.fragmject.feature.home.ui.project

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.example.fragmject.core.model.ProjectTree
import com.example.fragmject.core.domain.repository.ProjectRepository
import com.example.fragmject.core.domain.result.DomainResult
import androidx.lifecycle.ViewModel
import com.example.fragmject.core.common.utils.updateSuccessFrom
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

private const val TAG = "ProjectTreeVM"

@HiltViewModel
class ProjectTreeViewModel @Inject constructor(
    private val repo: ProjectRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProjectTreeUiState>(ProjectTreeUiState.Success())
    val uiState: StateFlow<ProjectTreeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repo.observeProjectTree().collect { trees ->
                _uiState.updateSuccessFrom({ ProjectTreeUiState.Success() }) { it.copy(isLoading = false, result = trees) }
            }
        }
        viewModelScope.launch {
            when (val r = repo.refreshProjectTree()) {
                is DomainResult.Success -> Unit
                is DomainResult.Failure -> Log.e(TAG, "refreshProjectTree failed: ${r.code} ${r.message}")
            }
        }
    }
}
