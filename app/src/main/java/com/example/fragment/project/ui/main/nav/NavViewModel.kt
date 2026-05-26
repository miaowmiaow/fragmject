package com.example.fragment.project.ui.main.nav

import androidx.lifecycle.viewModelScope
import com.example.fragment.project.data.Navigation
import com.example.fragment.project.data.Tree
import com.example.fragment.project.data.repository.CommonRepository
import com.example.fragment.project.data.repository.WanRepositoryProvider
import com.example.miaow.base.vm.BaseViewModel
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

    /**
     * 拉取导航与体系树。两个接口独立走 SWR：缓存命中先上屏，再被网络结果刷新。
     *
     * 关于 isLoading：UI 上的转圈仅服务于"导航"页签首屏（NavLinkContent），
     * 体系页签自身用 systemData.isEmpty() 判空，无需依赖此字段。
     * 因此只要 navigation 任一阶段（缓存或网络）有数据上屏，就立即关闭 loading，
     * 避免出现"已经渲染数据却仍挂着转圈"的割裂体验。
     */
    private fun getHome() {
        _uiState.update {
            it.copy(isLoading = true)
        }
        viewModelScope.launch {
            commonRepo.getNavigationFlow().collect { result ->
                val response = result.value
                _uiState.update { state ->
                    state.copy(
                        // 数据上屏即关闭 loading（缓存命中也算）
                        isLoading = if (response.data != null) false else state.isLoading,
                        navigationResult = response.data ?: state.navigationResult,
                    )
                }
            }
        }
        viewModelScope.launch {
            commonRepo.getSystemTreeFlow().collect { result ->
                val response = result.value
                _uiState.update { state ->
                    state.copy(
                        systemTreeResult = response.data ?: state.systemTreeResult,
                    )
                }
            }
        }
    }
}