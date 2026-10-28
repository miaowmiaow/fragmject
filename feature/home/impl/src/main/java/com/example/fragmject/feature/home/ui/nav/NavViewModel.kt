package com.example.fragmject.feature.home.ui.nav

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fragmject.core.domain.repository.HomeNavRepository
import com.example.fragmject.core.domain.repository.SystemRepository
import com.example.fragmject.core.model.NavTab
import com.example.fragmject.core.model.Tree
import com.example.fragmject.core.ui.utils.RefreshableUiState
import com.example.fragmject.core.ui.utils.collectAndRefresh
import com.example.fragmject.core.ui.utils.retryRefresh
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class NavViewModel @Inject constructor(
    private val homeNavRepository: HomeNavRepository,
    private val systemRepository: SystemRepository,
) : ViewModel() {

    private val _navState =
        MutableStateFlow<RefreshableUiState<List<NavTab>>>(RefreshableUiState.Loading)
    val navState: StateFlow<RefreshableUiState<List<NavTab>>> = _navState.asStateFlow()

    private val _treeState =
        MutableStateFlow<RefreshableUiState<List<Tree>>>(RefreshableUiState.Loading)
    val treeState: StateFlow<RefreshableUiState<List<Tree>>> = _treeState.asStateFlow()

    init {
        viewModelScope.collectAndRefresh(
            observe = homeNavRepository.observeNavigation(),
            refresh = homeNavRepository::refreshNavigation,
            state = _navState,
            isEmpty = { it.isEmpty() },
        )
        viewModelScope.collectAndRefresh(
            observe = systemRepository.observeSystemTree(),
            refresh = systemRepository::refreshSystemTree,
            state = _treeState,
            isEmpty = { it.isEmpty() },
        )
    }

    /** 导航链接错误态重试。 */
    fun retry() {
        viewModelScope.retryRefresh(
            observe = homeNavRepository.observeNavigation(),
            refresh = homeNavRepository::refreshNavigation,
            state = _navState,
        )
    }

    /** 体系树错误态重试。 */
    fun retryTree() {
        viewModelScope.retryRefresh(
            observe = systemRepository.observeSystemTree(),
            refresh = systemRepository::refreshSystemTree,
            state = _treeState,
        )
    }
}
