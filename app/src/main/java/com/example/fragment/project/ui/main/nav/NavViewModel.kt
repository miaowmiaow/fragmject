package com.example.fragment.project.ui.main.nav

import androidx.lifecycle.viewModelScope
import com.example.fragment.project.data.Navigation
import com.example.fragment.project.data.Tree
import com.example.fragment.project.data.repository.CommonRepository
import com.example.fragment.project.data.repository.WanRepositoryProvider
import com.example.miaow.base.vm.BaseViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NavUiState(
    val isLoading: Boolean = false,
    val navigationResult: List<Navigation> = emptyList(),
    val systemTreeResult: List<Tree> = emptyList(),
)

class NavViewModel(
    private val commonRepo: CommonRepository = WanRepositoryProvider.common,
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(NavUiState())

    val uiState: StateFlow<NavUiState> = _uiState.asStateFlow()

    init {
        getHome()
    }

    private fun getHome() {
        _uiState.update {
            it.copy(isLoading = true)
        }
        viewModelScope.launch {
            //获取导航数据
            val navi = async { commonRepo.getNavigation() }
            val tree = async { commonRepo.getSystemTree() }
            val naviData = navi.await().data
            val treeData = tree.await().data
            _uiState.update { state ->
                state.copy(
                    isLoading = false,
                    navigationResult = naviData ?: state.navigationResult,
                    systemTreeResult = treeData ?: state.systemTreeResult,
                )
            }
        }
    }
}