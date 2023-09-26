package com.example.fragment.project

import androidx.lifecycle.viewModelScope
import com.example.fragment.project.bean.HotKeyBean
import com.example.fragment.project.bean.HotKeyListBean
import com.example.fragment.project.bean.TreeBean
import com.example.fragment.project.bean.TreeListBean
import com.example.fragment.project.utils.WanHelper
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
    var hotKeyResult: MutableList<HotKeyBean> = ArrayList(),
    var treeResult: MutableList<TreeBean> = ArrayList(),
    var searchHistoryResult: MutableList<String> = ArrayList(),
    var webBrowseResult: MutableList<String> = ArrayList(),
    var webCollectResult: MutableList<String> = ArrayList(),
    var updateTime: Long = 0
)

class WanViewModel : BaseViewModel() {

    private val _uiState = MutableStateFlow(WanUiState())

    val uiState: StateFlow<WanUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val hotKeyList = async { getHotKeyList() }
            val treeList = async { getTreeList() }
            val searchHistoryList = async { WanHelper.getSearchHistory() }
            val webBrowseList = async { WanHelper.getWebBrowse() }
            val webCollectList = async { WanHelper.getWebCollect() }
            _uiState.update { state ->
                hotKeyList.await().data?.let { data ->
                    state.hotKeyResult.clear()
                    state.hotKeyResult.addAll(data)
                }
                treeList.await().data?.let { data ->
                    state.treeResult.clear()
                    state.treeResult.addAll(data)
                }
                searchHistoryList.await().let {
                    state.searchHistoryResult.clear()
                    state.searchHistoryResult.addAll(it)
                }
                webBrowseList.await().let {
                    state.webBrowseResult.clear()
                    state.webBrowseResult.addAll(it)
                }
                webCollectList.await().let {
                    state.webCollectResult.clear()
                    state.webCollectResult.addAll(it)
                }
                state.copy(updateTime = System.nanoTime())
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

    fun onSearchHistory(isAdd: Boolean, text: String) {
        _uiState.update {
            if (it.searchHistoryResult.contains(text)) {
                it.searchHistoryResult.remove(text)
            }
            if (isAdd) {
                it.searchHistoryResult.add(0, text)
            }
            it.copy(updateTime = System.nanoTime())
        }
    }

    fun onWebBrowse(isAdd: Boolean, text: String) {
        _uiState.update {
            if (it.webBrowseResult.contains(text)) {
                it.webBrowseResult.remove(text)
            }
            if (isAdd) {
                it.webBrowseResult.add(0, text)
            }
            it.copy(updateTime = System.nanoTime())
        }
    }

    fun onWebCollect(isAdd: Boolean, text: String) {
        _uiState.update {
            if (it.webCollectResult.contains(text)) {
                it.webCollectResult.remove(text)
            }
            if (isAdd) {
                it.webCollectResult.add(0, text)
            }
            it.copy(updateTime = System.nanoTime())
        }
    }

    fun onSaveWanHelper() {
        WanHelper.setSearchHistory(_uiState.value.searchHistoryResult)
        WanHelper.setWebBrowse(_uiState.value.webBrowseResult)
        WanHelper.setWebCollect(_uiState.value.webCollectResult)
    }

}