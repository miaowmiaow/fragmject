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
        getFirstPageWithCache(cid, getHomePage(key = cid))
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
     * 首页（page=0）走 SWR：先缓存上屏，再被网络刷新。
     * cacheKey 包含 cid + page，避免不同分类互相污染。
     */
    private fun getFirstPageWithCache(cid: String, page: Int) {
        viewModelScope.launch {
            articleRepo.getArticleListByCidFlow(cid, page).collect { result ->
                val response = result.value
                val networkArrived = !result.fromCache
                if (networkArrived) {
                    // 仅在网络阶段更新分页元数据与转场动画信号，避免缓存阶段提前触发
                    updatePageCont(response.data?.pageCount?.toInt(), cid)
                    if (_uiState.value.result[cid].isNullOrEmpty()) {
                        transitionAnimationEnd(response.time)
                    }
                }
                _uiState.update { state ->
                    val datas = response.data?.datas.orEmpty()
                    state.copy(
                        // 缓存阶段不刷新分页相关 UI 标志，等网络回来再统一更新
                        isRefreshing = if (networkArrived) state.isRefreshing + (cid to false) else state.isRefreshing,
                        isLoading = if (networkArrived) state.isLoading + (cid to hasNextPage(cid)) else state.isLoading,
                        isFinishing = if (networkArrived) state.isFinishing + (cid to !hasNextPage(cid)) else state.isFinishing,
                        // 首页接口直接覆盖，避免缓存数据与网络数据重复拼接
                        result = state.result + (cid to datas),
                    )
                }
            }
        }
    }

    /**
     * 获取知识体系下的文章（仅用于分页，不接入缓存）
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