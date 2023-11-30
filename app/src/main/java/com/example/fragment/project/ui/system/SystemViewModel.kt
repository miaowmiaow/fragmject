package com.example.fragment.project.ui.system

import androidx.lifecycle.viewModelScope
import com.example.fragment.project.data.Article
import com.example.fragment.project.data.ArticleList
import com.example.miaow.base.http.get
import com.example.miaow.base.vm.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SystemUiState(
    val refreshing: MutableMap<String, Boolean> = HashMap(),
    val loading: MutableMap<String, Boolean> = HashMap(),
    val finishing: MutableMap<String, Boolean> = HashMap(),
    val result: MutableMap<String, ArrayList<Article>> = HashMap(),
    var updateTime: Long = 0
) {
    fun getRefreshing(cid: String): Boolean {
        return refreshing[cid] ?: true
    }

    fun getLoading(cid: String): Boolean {
        return loading[cid] ?: false
    }

    fun getFinishing(cid: String): Boolean {
        return finishing[cid] ?: false
    }

    fun getResult(cid: String): ArrayList<Article>? {
        return result[cid]
    }

}

class SystemViewModel : BaseViewModel() {

    private val _uiState = MutableStateFlow(SystemUiState())

    val uiState: StateFlow<SystemUiState> = _uiState.asStateFlow()

    fun init(cid: String) {
        if (!uiState.value.result.containsKey(cid)) {
            getHome(cid)
        }
    }

    fun getHome(cid: String) {
        _uiState.update {
            it.refreshing[cid] = true
            it.loading[cid] = false
            it.finishing[cid] = false
            it.copy(updateTime = System.nanoTime())
        }
        getList(cid, getHomePage(key = cid))
    }

    fun getNext(cid: String) {
        _uiState.update {
            it.refreshing[cid] = false
            it.loading[cid] = false
            it.finishing[cid] = false
            it.copy(updateTime = System.nanoTime())
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
            val response = get<ArticleList> {
                setUrl("article/list/{page}/json")
                putPath("page", page.toString())
                putQuery("cid", cid)
            }
            updatePageCont(response.data?.pageCount?.toInt(), cid)
            _uiState.update { state ->
                //response.isNullOrEmpty()，则在转场动画结束后加载数据，用于解决过度动画卡顿问题
                if (state.result[cid].isNullOrEmpty()) {
                    transitionAnimationEnd(response.time)
                }
                response.data?.datas?.let { datas ->
                    if (isHomePage(cid)) {
                        state.result[cid] = arrayListOf()
                    }
                    state.result[cid]?.addAll(datas)
                }
                state.refreshing[cid] = false
                state.loading[cid] = hasNextPage(cid)
                state.finishing[cid] = !hasNextPage(cid)
                state.copy(updateTime = System.nanoTime())
            }
        }
    }

}