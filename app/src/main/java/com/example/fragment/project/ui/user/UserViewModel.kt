package com.example.fragment.project.ui.user

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.fragment.project.bean.ArticleBean
import com.example.fragment.project.bean.CoinBean
import com.example.fragment.project.bean.ShareArticleListBean
import com.example.miaow.base.http.get
import com.example.miaow.base.vm.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UserUiState(
    var refreshing: Boolean = false,
    var loading: Boolean = false,
    var finishing: Boolean = false,
    var coinResult: CoinBean = CoinBean(),
    var articleResult: MutableList<ArticleBean> = ArrayList(),
)

class UserViewModel(private val id: String) : BaseViewModel() {

    private val _uiState = MutableStateFlow(UserUiState())

    val uiState: StateFlow<UserUiState> = _uiState.asStateFlow()

    init {
        getHome()
    }

    fun getHome() {
        _uiState.update {
            it.copy(refreshing = true, loading = false, finishing = false)
        }
        getShareArticlesList(getHomePage(1))
    }

    fun getNext() {
        _uiState.update {
            it.copy(refreshing = false, loading = false, finishing = false)
        }
        getShareArticlesList(getNextPage())
    }

    /**
     * 获取用户分享文章
     * page 1开始
     */
    private fun getShareArticlesList(page: Int) {
        viewModelScope.launch {
            val response = get<ShareArticleListBean> {
                setUrl("user/{id}/share_articles/{page}/json")
                putPath("id", id)
                putPath("page", page.toString())
            }
            updatePageCont(response.data?.shareArticles?.pageCount?.toInt())
            _uiState.update { state ->
                response.data?.coinInfo?.let { coin ->
                    state.coinResult = coin
                }
                response.data?.shareArticles?.datas?.let { datas ->
                    if (isHomePage()) {
                        state.articleResult.clear()
                    }
                    state.articleResult.addAll(datas)
                }
                if (response.data == null) {
                    state.coinResult.username = response.errorMsg
                }
                state.copy(refreshing = false, loading = hasNextPage(), finishing = !hasNextPage())
            }
        }
    }

    companion object {
        fun provideFactory(userId: String): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return UserViewModel(userId) as T
                }
            }
    }
}