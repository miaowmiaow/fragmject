package com.example.fragment.project.ui.main.nav

import androidx.lifecycle.viewModelScope
import com.example.fragment.project.bean.ArticleBean
import com.example.fragment.project.bean.NavigationBean
import com.example.fragment.project.bean.NavigationListBean
import com.example.fragment.project.bean.TreeBean
import com.example.fragment.project.bean.TreeListBean
import com.example.miaow.base.http.HttpRequest
import com.example.miaow.base.http.get
import com.example.miaow.base.vm.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NavState(
    var isLoading: Boolean = false,
    var currentPosition: Int = 0,
    var navigationResult: MutableList<NavigationBean> = ArrayList(),
    var articlesResult: MutableList<ArticleBean> = ArrayList(),
    var systemTreeResult: MutableList<TreeBean> = ArrayList(),
)

class NavViewModel : BaseViewModel() {

    private val _uiState = MutableStateFlow(NavState())

    val uiState: StateFlow<NavState> = _uiState.asStateFlow()

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
                state.articlesResult.clear()
                state.articlesResult.addAll(articles)
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
            val request = HttpRequest("navi/json")
            val response = get<NavigationListBean>(request)
            _uiState.update { state ->
                response.data?.let { data ->
                    state.navigationResult.clear()
                    state.navigationResult.addAll(data)
                    updateSelectNavigation(0)
                }
                state.copy(isLoading = false)
            }
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
            val request = HttpRequest("tree/json")
            val response = get<TreeListBean>(request)
            _uiState.update { state ->
                response.data?.let { data ->
                    state.systemTreeResult.clear()
                    state.systemTreeResult.addAll(data)
                }
                state.copy(isLoading = false)
            }
        }
    }

}