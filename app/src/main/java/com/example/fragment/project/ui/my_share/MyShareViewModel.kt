package com.example.fragment.project.ui.my_share

import androidx.lifecycle.viewModelScope
import com.example.fragment.project.bean.ArticleBean
import com.example.fragment.project.bean.ShareArticleListBean
import com.example.miaow.base.http.get
import com.example.miaow.base.vm.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MyShareUiState(
    var refreshing: Boolean = false,
    var loading: Boolean = false,
    var finishing: Boolean = false,
    var result: MutableList<ArticleBean> = ArrayList(),
)

class MyShareViewModel : BaseViewModel() {

    private val _uiState = MutableStateFlow(MyShareUiState())

    val uiState: StateFlow<MyShareUiState> = _uiState.asStateFlow()

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
     * 获取自己的分享的文章
     * page 1开始
     */
    private fun getList(page: Int) {
        viewModelScope.launch {
            val response = get<ShareArticleListBean> {
                setUrl("user/lg/private_articles/{page}/json")
                putPath("page", page.toString())
            }
            updatePageCont(response.data?.shareArticles?.pageCount?.toInt())
            _uiState.update { state ->
                response.data?.shareArticles?.datas?.let { datas ->
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