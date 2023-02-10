package com.example.fragment.module.wan.model

import androidx.lifecycle.viewModelScope
import com.example.fragment.library.base.http.HttpRequest
import com.example.fragment.library.base.http.get
import com.example.fragment.library.base.model.BaseViewModel
import com.example.fragment.library.common.bean.SystemTreeBean
import com.example.fragment.library.common.bean.SystemTreeListBean
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SystemTreeState(
    var loading: Boolean = false,
    var title: String = "体系",
    var result: MutableList<SystemTreeBean> = ArrayList(),
    var response: MutableList<SystemTreeBean> = ArrayList()
)

class SystemTreeViewModel : BaseViewModel() {

    private val _uiState = MutableStateFlow(SystemTreeState())

    val uiState: StateFlow<SystemTreeState> = _uiState

    fun init(cid: String) {
        _uiState.update {
            it.copy(loading = true)
        }
        if (uiState.value.response.isEmpty()) {
            getSystemTree(cid)
        } else {
            setSystemTree(cid)
        }

    }

    /**
     * 获取项目分类
     */
    private fun getSystemTree(cid: String) {
        viewModelScope.launch {
            val request = HttpRequest("tree/json")
            val response = get<SystemTreeListBean>(request) { updateProgress(it) }
            response.data?.let { data ->
                _uiState.value.response.clear()
                _uiState.value.response.addAll(data)
            }
            setSystemTree(cid)
        }
    }

    private fun setSystemTree(cid: String) {
        _uiState.update {
            it.response.forEach { data ->
                data.children?.forEachIndexed { index, children ->
                    if (children.id == cid) {
                        updateTabIndex(index)
                        it.title = data.name
                        it.result.clear()
                        it.result.addAll(data.children!!)
                    }
                }
            }
            it.copy(loading = false)
        }
    }

}