package com.example.fragment.project

import androidx.lifecycle.viewModelScope
import com.example.fragment.project.data.HotKey
import com.example.fragment.project.data.HotKeyList
import com.example.fragment.project.data.Tree
import com.example.fragment.project.data.TreeList
import com.example.miaow.base.http.get
import com.example.miaow.base.vm.BaseViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WanUiState(
    var hotKeyResult: List<HotKey> = ArrayList(),
    var treeResult: List<Tree> = ArrayList(),
    var isLoading: Boolean = false,
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

class WanViewModel : BaseViewModel() {

    private val _uiState = MutableStateFlow(WanUiState())

    val uiState: StateFlow<WanUiState> = _uiState.asStateFlow()

    init {
        _uiState.update {
            it.copy(isLoading = true)
        }
        viewModelScope.launch {
            val hotKeyList = async { getHotKeyList() }
            val treeList = async { getTreeList() }
            _uiState.update { state ->
                hotKeyList.await().data?.let { data ->
                    state.hotKeyResult = data
                }
                treeList.await().data?.let { data ->
                    state.treeResult = data
                }
                state.copy(isLoading = false)
            }
        }
    }

    /**
     * 获取搜索热词
     */
    private suspend fun getHotKeyList(): HotKeyList {
        return coroutineScope {
            get {
                setUrl("hotkey/json")
            }
        }
    }

    /**
     * 获取项目分类
     */
    private suspend fun getTreeList(): TreeList {
        return coroutineScope {
            get {
                setUrl("tree/json")
            }
        }
    }

}