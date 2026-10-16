package com.example.fragmject.feature.home.ui.project

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fragmject.core.domain.repository.ProjectRepository
import com.example.fragmject.core.model.ProjectTree
import com.example.fragmject.core.ui.utils.RefreshableUiState
import com.example.fragmject.core.ui.utils.collectAndRefresh
import com.example.fragmject.core.ui.utils.retryRefresh
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class ProjectTreeViewModel @Inject constructor(
    private val repo: ProjectRepository,
) : ViewModel() {

    private val _uiState =
        MutableStateFlow<RefreshableUiState<List<ProjectTree>>>(RefreshableUiState.Loading)
    val uiState: StateFlow<RefreshableUiState<List<ProjectTree>>> = _uiState.asStateFlow()

    init {
        viewModelScope.collectAndRefresh(
            observe = repo.observeProjectTree(),
            refresh = repo::refreshProjectTree,
            state = _uiState,
        )
    }

    fun refresh() {
        viewModelScope.retryRefresh(repo::refreshProjectTree, _uiState)
    }
}
