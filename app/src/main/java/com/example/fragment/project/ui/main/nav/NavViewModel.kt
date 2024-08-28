package com.example.fragment.project.ui.main.nav

import androidx.lifecycle.viewModelScope
import com.example.fragment.project.data.Navigation
import com.example.fragment.project.data.NavigationList
import com.example.fragment.project.data.Tree
import com.example.fragment.project.data.TreeList
import com.example.miaow.base.http.get
import com.example.miaow.base.vm.BaseViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NavUiState(
    var isLoading: Boolean = false,
    var navigationResult: MutableList<Navigation> = ArrayList(),
    var systemTreeResult: MutableList<Tree> = ArrayList(),
)

class NavViewModel : BaseViewModel() {

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
            val navi = async {
                get<NavigationList> {
                    setUrl("navi/json")
                }
            }
            val tree = async {
                get<TreeList> {
                    setUrl("tree/json")
                }
            }
            _uiState.update { state ->
                navi.await().data?.let { data ->
                    state.navigationResult = data
                }
                tree.await().data?.let { data ->
                    state.systemTreeResult = data
                }
                state.copy(isLoading = false)
            }

        }
    }
}