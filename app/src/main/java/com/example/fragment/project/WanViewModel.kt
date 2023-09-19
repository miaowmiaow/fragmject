package com.example.fragment.project

import androidx.lifecycle.viewModelScope
import com.example.fragment.project.bean.HotKeyBean
import com.example.fragment.project.bean.HotKeyListBean
import com.example.fragment.project.bean.TreeBean
import com.example.fragment.project.bean.TreeListBean
import com.example.miaow.base.http.HttpRequest
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
    var isLoading: Boolean = false,
    var hotKeyResult: MutableList<HotKeyBean> = ArrayList(),
    var treeResult: MutableList<TreeBean> = ArrayList(),
)

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
                    state.hotKeyResult.clear()
                    state.hotKeyResult.addAll(data)
                }
                treeList.await().data?.let { data ->
                    state.treeResult.clear()
                    state.treeResult.addAll(data)
                }
                state.copy(isLoading = false)
            }
        }
    }

    /**
     * 获取搜索热词
     */
    private suspend fun getHotKeyList(): HotKeyListBean {
        return coroutineScope { get(HttpRequest("hotkey/json")) }
    }

    /**
     * 获取项目分类
     */
    private suspend fun getTreeList(): TreeListBean {
        return coroutineScope { get(HttpRequest("tree/json")) }
    }

}