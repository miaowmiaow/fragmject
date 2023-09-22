package com.example.fragment.project.ui.my_browse

import com.example.fragment.project.utils.WanHelper
import com.example.miaow.base.vm.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class MyBrowseUiState(
    var historyResult: MutableList<String> = ArrayList(),
    var updateTime: Long = 0
)

class MyBrowseViewModel : BaseViewModel() {

    private val _uiState = MutableStateFlow(MyBrowseUiState())

    val uiState: StateFlow<MyBrowseUiState> = _uiState.asStateFlow()

    fun initHistoryWeb() {
        WanHelper.getHistoryWeb { history ->
            _uiState.update {
                it.historyResult.clear()
                it.historyResult.addAll(history)
                it.copy(updateTime = System.nanoTime())
            }
        }
    }

    fun updateHistoryWeb(key: String) {
        _uiState.update {
            if (it.historyResult.contains(key)) {
                it.historyResult.remove(key)
            }
            it.historyResult.add(0, key)
            WanHelper.setHistoryWeb(it.historyResult)
            it.copy(updateTime = System.nanoTime())
        }
    }

    fun removeHistoryWeb(key: String) {
        _uiState.update {
            if (it.historyResult.contains(key)) {
                it.historyResult.remove(key)
            }
            WanHelper.setHistoryWeb(it.historyResult)
            it.copy(updateTime = System.nanoTime())
        }
    }

}