package com.example.fragment.project

import androidx.lifecycle.viewModelScope
import com.example.fragment.project.data.HotKey
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

/**
 * 不可变 UiState：所有字段使用 val + 不可变集合，
 * 避免直接突变 data class 字段导致 StateFlow 订阅方收不到变化。
 */
data class WanUiState(
    val hotKeyResult: List<HotKey> = emptyList(),
    val treeResult: List<Tree> = emptyList(),
    val isLoading: Boolean = false,
) {

    fun getTree(cid: String): Triple<Int, String, List<Tree>> {
        treeResult.forEach { tree ->
            tree.children?.forEachIndexed { index, data ->
                if (data.id == cid) {
                    return Triple(index, tree.name, tree.children)
                }
            }
        }
        return Triple(0, "体系", listOf())
    }
}

class WanViewModel(
    private val commonRepo: CommonRepository = WanRepositoryProvider.common,
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(WanUiState())

    val uiState: StateFlow<WanUiState> = _uiState.asStateFlow()

    init {
        _uiState.update {
            it.copy(isLoading = true)
        }
        viewModelScope.launch {
            val hotKeyList = async { commonRepo.getHotKey() }
            val treeList = async { commonRepo.getSystemTree() }
            val hotKeyData = hotKeyList.await().data
            val treeData = treeList.await().data
            _uiState.update { state ->
                state.copy(
                    isLoading = false,
                    hotKeyResult = hotKeyData ?: state.hotKeyResult,
                    treeResult = treeData ?: state.treeResult,
                )
            }
        }
    }

}