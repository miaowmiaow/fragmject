package com.example.fragmject.feature.wan

import androidx.lifecycle.viewModelScope
import com.example.fragmject.core.model.HotKey
import com.example.fragmject.core.model.Tree
import com.example.fragmject.core.common.viewmodel.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 不可变 UiState：所有字段使用 val + 不可变集合，
 * 避免直接突变 data class 字段导致 StateFlow 订阅方收不到变化。
 */
sealed interface WanUiState {
    data class Success(
        val hotKeyResult: List<HotKey> = emptyList(),
        val treeResult: List<Tree> = emptyList(),
        val isLoading: Boolean = false,
    ) : WanUiState {
        fun getTree(cid: String): Triple<Int, String, List<Tree>> {
            treeResult.forEach { tree ->
                tree.children?.forEachIndexed { index, data ->
                    if (data.id == cid) return Triple(index, tree.name, tree.children ?: listOf())
                }
            }
            return Triple(0, "体系", listOf())
        }
    }
}

// Screen accessors
val WanUiState.hotKeyResult get() = (this as? WanUiState.Success)?.hotKeyResult ?: emptyList()
val WanUiState.treeResult get() = (this as? WanUiState.Success)?.treeResult ?: emptyList()
val WanUiState.isLoading get() = (this as? WanUiState.Success)?.isLoading ?: false

private inline fun MutableStateFlow<WanUiState>.updateData(
    crossinline block: (WanUiState.Success) -> WanUiState.Success,
) {
    update { if (it is WanUiState.Success) block(it) else it }
}

@HiltViewModel
class WanViewModel @Inject constructor(
    private val offlineFirst: com.example.fragmject.core.data.repository.OfflineFirstCommonRepository,
) : BaseViewModel() {

    private val _uiState = MutableStateFlow<WanUiState>(WanUiState.Success(isLoading = true))
    val uiState: StateFlow<WanUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            offlineFirst.observeHotKey().collect { hotKeys ->
                _uiState.updateData { it.copy(hotKeyResult = hotKeys) }
            }
        }
        viewModelScope.launch {
            offlineFirst.observeSystemTree().collect { trees ->
                _uiState.updateData { it.copy(treeResult = trees) }
            }
        }
        viewModelScope.launch {
            offlineFirst.refreshAll()
            _uiState.updateData { it.copy(isLoading = false) }
        }
    }
}