package com.example.fragment.project.ui.my_share

import androidx.lifecycle.viewModelScope
import com.example.fragment.project.data.Article
import com.example.fragment.project.data.ShareArticleList
import com.example.miaow.base.http.get
import com.example.miaow.base.vm.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MyShareUiState(
    var isRefreshing: Boolean = false,
    var isLoading: Boolean = false,
    var isFinishing: Boolean = false,
    var result: MutableList<Article> = ArrayList(),
)

class MyShareViewModel : BaseViewModel() {

    private val _uiState = MutableStateFlow(MyShareUiState())

    val uiState: StateFlow<MyShareUiState> = _uiState.asStateFlow()

    init {
        getHome()
    }

    fun getHome() {
        _uiState.update {
            it.copy(isRefreshing = true, isLoading = false, isFinishing = false)
        }
        getList(getHomePage())
    }

    fun getNext() {
        _uiState.update {
            it.copy(isRefreshing = false, isLoading = false, isFinishing = false)
        }
        getList(getNextPage())
    }


    /**
     * 获取自己的分享的文章
     * page 1开始
     */
    private fun getList(page: Int) {
        viewModelScope.launch {
            val response = get<ShareArticleList> {
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
                    isRefreshing = false,
                    isLoading = hasNextPage(),
                    isFinishing = !hasNextPage()
                )
            }
        }
    }

}