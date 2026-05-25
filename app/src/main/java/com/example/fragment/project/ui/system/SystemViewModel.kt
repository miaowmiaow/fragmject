package com.example.fragment.project.ui.system

import androidx.lifecycle.viewModelScope
import com.example.fragment.project.data.Article
import com.example.fragment.project.data.repository.ArticleRepository
import com.example.fragment.project.data.repository.WanRepositoryProvider
import com.example.miaow.base.vm.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SystemUiState(
    val isRefreshing: Map<String, Boolean> = emptyMap(),
    val isLoading: Map<String, Boolean> = emptyMap(),
    val isFinishing: Map<String, Boolean> = emptyMap(),
    val result: Map<String, List<Article>> = emptyMap(),
) {
    fun getRefreshing(cid: String): Boolean {
        return isRefreshing[cid] ?: true
    }

    fun getLoading(cid: String): Boolean {
        return isLoading[cid] ?: false
    }

    fun getFinishing(cid: String): Boolean {
        return isFinishing[cid] ?: false
    }

    fun getResult(cid: String): List<Article>? {
        return result[cid]
    }

}

class SystemViewModel(
    private val articleRepo: ArticleRepository = WanRepositoryProvider.article,
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(SystemUiState())

    val uiState: StateFlow<SystemUiState> = _uiState.asStateFlow()

    fun init(cid: String) {
        if (!uiState.value.result.containsKey(cid)) {
            getHome(cid)
        }
    }

    fun getHome(cid: String) {
        _uiState.update { state ->
            state.copy(
                isRefreshing = state.isRefreshing + (cid to true),
                isLoading = state.isLoading + (cid to false),
                isFinishing = state.isFinishing + (cid to false),
            )
        }
        getList(cid, getHomePage(key = cid))
    }

    fun getNext(cid: String) {
        _uiState.update { state ->
            state.copy(
                isRefreshing = state.isRefreshing + (cid to false),
                isLoading = state.isLoading + (cid to false),
                isFinishing = state.isFinishing + (cid to false),
            )
        }
        getList(cid, getNextPage(cid))
    }

    /**
     * 获取知识体系下的文章
     * 	cid 分类id
     * 	page 0开始
     */
    private fun getList(cid: String, page: Int) {
        viewModelScope.launch {
            val response = articleRepo.getArticleListByCid(cid, page)
            updatePageCont(response.data?.pageCount?.toInt(), cid)
            //response.isNullOrEmpty()，则在转场动画结束后加载数据，用于解决过度动画卡顿问题
            if (_uiState.value.result[cid].isNullOrEmpty()) {
                transitionAnimationEnd(response.time)
            }
            _uiState.update { state ->
                val datas = response.data?.datas.orEmpty()
                val previous = state.result[cid].orEmpty()
                val merged = if (isHomePage(cid)) datas else previous + datas
                state.copy(
                    isRefreshing = state.isRefreshing + (cid to false),
                    isLoading = state.isLoading + (cid to hasNextPage(cid)),
                    isFinishing = state.isFinishing + (cid to !hasNextPage(cid)),
                    result = state.result + (cid to merged),
                )
            }
        }
    }

}