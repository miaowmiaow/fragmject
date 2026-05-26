package com.example.fragment.project.ui.main.project

import androidx.lifecycle.viewModelScope
import com.example.fragment.project.data.Article
import com.example.fragment.project.data.repository.ProjectRepository
import com.example.fragment.project.data.repository.WanRepositoryProvider
import com.example.fragment.project.data.repository.collectCached
import com.example.miaow.base.vm.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 不可变 UiState：通过整体替换 Map 引用来触发 StateFlow 的变化通知，
 * 不再依赖 updateTime = System.nanoTime() 这种 hack。
 */
data class ProjectUiState(
    val isRefreshing: Map<String, Boolean> = emptyMap(),
    val isLoading: Map<String, Boolean> = emptyMap(),
    val isFinishing: Map<String, Boolean> = emptyMap(),
    val result: Map<String, List<Article>> = emptyMap(),
) {
    fun getRefreshing(cid: String): Boolean {
        return isRefreshing[cid] ?: false
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

class ProjectViewModel(
    private val projectRepo: ProjectRepository = WanRepositoryProvider.project,
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(ProjectUiState())

    val uiState: StateFlow<ProjectUiState> = _uiState.asStateFlow()

    fun init(cid: String) {
        if (!uiState.value.result.containsKey(cid)) {
            getHome(cid)
        }
    }

    /**
     * @param userTriggered 是否由用户主动触发（下拉刷新）。
     *  - true：保留 isRefreshing 直到网络返回，给出明确的"刷新中"反馈；
     *  - false（默认）：自动加载场景。缓存命中后立即把页面当作"已就绪"，
     *    避免出现"缓存数据已渲染、顶部却仍挂着下拉刷新动画"的体验割裂。
     */
    fun getHome(cid: String, userTriggered: Boolean = false) {
        _uiState.update { state ->
            state.copy(
                isRefreshing = state.isRefreshing + (cid to userTriggered),
                isLoading = state.isLoading + (cid to false),
                isFinishing = state.isFinishing + (cid to false),
            )
        }
        getFirstPageWithCache(cid, getHomePage(1, cid))
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
     * 首页（page=1）接入 SWR：先缓存上屏，再被网络结果覆盖。
     * 通过 [collectCached] 算子统一缓存阶段 / 网络阶段的副作用边界。
     */
    private fun getFirstPageWithCache(cid: String, page: Int) {
        viewModelScope.launch {
            projectRepo.getProjectListFlow(cid, page).collectCached(
                onCache = { response ->
                    val datas = response.data?.datas.orEmpty()
                    _uiState.update { state ->
                        // 缓存阶段：仅替换数据，不动分页 / 刷新标志，避免基于过期 pageCount 误判
                        state.copy(result = state.result + (cid to datas))
                    }
                },
                onNetwork = { response ->
                    updatePageCont(response.data?.pageCount?.toInt(), cid)
                    val datas = response.data?.datas.orEmpty()
                    _uiState.update { state ->
                        state.copy(
                            isRefreshing = state.isRefreshing + (cid to false),
                            isLoading = state.isLoading + (cid to hasNextPage(cid)),
                            isFinishing = state.isFinishing + (cid to !hasNextPage(cid)),
                            result = state.result + (cid to datas),
                        )
                    }
                },
            )
        }
    }

    /**
     * 获取项目列表（仅用于分页，不接入缓存）
     * cid 分类id
     * page 1开始
     */
    private fun getList(cid: String, page: Int) {
        viewModelScope.launch {
            val response = projectRepo.getProjectList(cid, page)
            updatePageCont(response.data?.pageCount?.toInt(), cid)
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