package com.example.fragment.project.ui.my_collect

import androidx.lifecycle.viewModelScope
import com.example.fragment.project.bean.ArticleBean
import com.example.fragment.project.bean.ArticleListBean
import com.example.miaow.base.http.get
import com.example.miaow.base.vm.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MyCollectUiState(
    var refreshing: Boolean = false,
    var loading: Boolean = false,
    var finishing: Boolean = false,
    var result: MutableList<ArticleBean> = ArrayList(),
)

class MyCollectViewModel : BaseViewModel() {

    private val _uiState = MutableStateFlow(MyCollectUiState())

    val uiState: StateFlow<MyCollectUiState> = _uiState.asStateFlow()

    init {
        getHome()
    }

    fun getHome() {
        _uiState.update {
            it.copy(refreshing = true, loading = false, finishing = false)
        }
        getList(getHomePage())
    }

    fun getNext() {
        _uiState.update {
            it.copy(refreshing = false, loading = false, finishing = false)
        }
        getList(getNextPage())
    }


    /**
     * 获取收藏文章
     * page 0开始
     */
    private fun getList(page: Int) {
        viewModelScope.launch {
            val response = get<ArticleListBean> {
                setUrl("lg/collect/list/{page}/json")
                putPath("page", page.toString())
            }
            updatePageCont(response.data?.pageCount?.toInt())
            response.data?.let { data ->
                _uiState.update { state ->
                    data.datas?.let { datas ->
                        if (isHomePage()) {
                            state.result.clear()
                        }
                        state.result.addAll(datas)
                    }
                    state.copy(
                        refreshing = false,
                        loading = hasNextPage(),
                        finishing = !hasNextPage()
                    )
                }
            }
        }
    }

}