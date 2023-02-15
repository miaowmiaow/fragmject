package com.example.fragment.module.wan.model

import androidx.lifecycle.viewModelScope
import com.example.fragment.library.base.http.HttpRequest
import com.example.fragment.library.base.http.get
import com.example.fragment.library.base.model.BaseViewModel
import com.example.fragment.library.common.bean.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NavigationState(
    var isLoading: Boolean = false,
    var navigationResult: MutableList<NavigationBean> = ArrayList(),
    var articlesResult: MutableList<ArticleBean> = ArrayList(),
    var systemTreeResult: MutableList<SystemTreeBean> = ArrayList(),
    var currentPosition: Int = 0
)

class NavigationViewModel : BaseViewModel() {

    private val _uiState = MutableStateFlow(NavigationState())

    val uiState: StateFlow<NavigationState> = _uiState.asStateFlow()

    init {
        if (uiState.value.navigationResult.isEmpty()) {
            getNavigation()
        }
        if (uiState.value.systemTreeResult.isEmpty()) {
            getSystemTree()
        }
    }

    fun updateSelectNavigation(position: Int) {
        _uiState.update {
            it.navigationResult[it.currentPosition].isSelected = false
            val item = it.navigationResult[position]
            item.isSelected = true
            item.articles?.let { articles ->
                it.articlesResult.clear()
                it.articlesResult.addAll(articles)
            }
            it.copy(currentPosition = position)
        }
    }

    /**
     * 获取导航数据
     */
    private fun getNavigation() {
        _uiState.update {
            it.copy(isLoading = true)
        }
        //通过viewModelScope创建一个协程
        viewModelScope.launch {
            //构建请求体，传入请求参数
            val request = HttpRequest("navi/json")
            //以get方式发起网络请求
            val response = get<NavigationListBean>(request) { updateProgress(it) }
            _uiState.update {
                response.data?.let { data ->
                    it.navigationResult.clear()
                    it.navigationResult.addAll(data)
                    updateSelectNavigation(0)
                }
                it.copy(isLoading = false)
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
            val response = get<SystemTreeListBean>(request) { updateProgress(it) }
            _uiState.update {
                response.data?.let { data ->
                    it.systemTreeResult.clear()
                    it.systemTreeResult.addAll(data)
                }
                it.copy(isLoading = false)
            }
        }
    }

}