package com.example.fragment.project.ui.main.nav

import androidx.lifecycle.viewModelScope
import com.example.fragment.project.data.Article
import com.example.fragment.project.data.Navigation
import com.example.fragment.project.data.NavigationList
import com.example.fragment.project.data.Tree
import com.example.fragment.project.data.TreeList
import com.example.miaow.base.http.get
import com.example.miaow.base.vm.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NavUiState(
    var isLoading: Boolean = false,
    var currentPosition: Int = 0,
    var navigationResult: MutableList<Navigation> = ArrayList(),
    var articlesResult: MutableList<Article> = ArrayList(),
    var systemTreeResult: MutableList<Tree> = ArrayList(),
)

class NavViewModel : BaseViewModel() {

    private val _uiState = MutableStateFlow(NavUiState())

    val uiState: StateFlow<NavUiState> = _uiState.asStateFlow()

    init {
        if (uiState.value.navigationResult.isEmpty()) {
            getNavigation()
        }
        if (uiState.value.systemTreeResult.isEmpty()) {
            getSystemTree()
        }
    }

    fun updateSelectNavigation(position: Int) {
        _uiState.update { state ->
            state.navigationResult[state.currentPosition].isSelected = false
            val navigationResult = state.navigationResult[position]
            navigationResult.isSelected = true
            navigationResult.articles?.let { articles ->
                state.articlesResult = articles
            }
            state.copy(currentPosition = position)
        }
    }

    /**
     * 获取导航数据
     */
    private fun getNavigation() {
        _uiState.update {
            it.copy(isLoading = true)
        }
        viewModelScope.launch {
            val response = get<NavigationList> {
                setUrl("navi/json")
            }
            _uiState.update { state ->
                response.data?.let { data ->
                    state.navigationResult = data
                }
                state.copy(isLoading = false)
            }
            updateSelectNavigation(0)
        }
    }

    /**
     * 获取项目分类
     */
    private fun getSystemTree() {
        _uiState.update {
            it.copy(isLoading = true)
        }
        viewModelScope.launch {
            val response = get<TreeList> {
                setUrl("tree/json")
            }
            _uiState.update { state ->
                response.data?.let { data ->
                    state.systemTreeResult = data
                }
                state.copy(isLoading = false)
            }
        }
    }

}