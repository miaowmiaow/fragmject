package com.example.fragment.module.wan.model

import androidx.lifecycle.viewModelScope
import com.example.fragment.library.base.http.HttpRequest
import com.example.fragment.library.base.http.get
import com.example.fragment.library.base.model.BaseViewModel
import com.example.fragment.library.common.bean.HotKeyBean
import com.example.fragment.library.common.bean.HotKeyListBean
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HotKeyState(
    var isLoading: Boolean = false,
    var result: MutableList<HotKeyBean> = ArrayList(),
)

class HotKeyViewModel : BaseViewModel() {

    private val _uiState = MutableStateFlow(HotKeyState())

    val uiState: StateFlow<HotKeyState> = _uiState.asStateFlow()

    init {
        getHotKey()
    }

    /**
     * 获取搜索热词
     */
    private fun getHotKey() {
        _uiState.update {
            it.copy(isLoading = true)
        }
        viewModelScope.launch {
            val request = HttpRequest("hotkey/json")
            val response = get<HotKeyListBean>(request)
            _uiState.update {
                response.data?.let { data ->
                    it.result.clear()
                    it.result.addAll(data)
                }
                it.copy(isLoading = false)
            }
        }
    }

}