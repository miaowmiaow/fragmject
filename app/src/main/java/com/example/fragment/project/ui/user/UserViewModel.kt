package com.example.fragment.project.ui.user

import androidx.lifecycle.viewModelScope
import com.example.fragment.library.base.http.HttpRequest
import com.example.fragment.library.base.http.get
import com.example.fragment.library.base.vm.BaseViewModel
import com.example.fragment.project.bean.ArticleBean
import com.example.fragment.project.bean.CoinBean
import com.example.fragment.project.bean.ShareArticleListBean
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UserState(
    var refreshing: Boolean = false,
    var loading: Boolean = false,
    var coinResult: CoinBean = CoinBean(),
    var articleResult: MutableList<ArticleBean> = ArrayList(),
)

class UserViewModel : BaseViewModel() {

    private val _uiState = MutableStateFlow(UserState())

    val uiState: StateFlow<UserState> = _uiState.asStateFlow()

    fun getHome(id: String) {
        _uiState.update {
            it.copy(refreshing = true)
        }
        getList(id, getHomePage(1))
    }

    fun getNext(id: String) {
        _uiState.update {
            it.copy(loading = false)
        }
        getList(id, getNextPage())
    }

    /**
     * 获取用户分享文章
     * page 1开始
     */
    private fun getList(id: String, page: Int) {
        viewModelScope.launch {
            //构建请求体，传入请求参数
            val request = HttpRequest("user/{id}/share_articles/{page}/json")
                .putPath("id", id)
                .putPath("page", page.toString())
            val response = get<ShareArticleListBean>(request) { updateProgress(it) }
            response.data?.shareArticles?.pageCount?.let { updatePageCont(it.toInt()) }
            _uiState.update {
                response.data?.coinInfo?.let { coin ->
                    it.coinResult = coin
                }
                response.data?.shareArticles?.datas?.let { datas ->
                    if (isHomePage()) {
                        it.articleResult.clear()
                    }
                    it.articleResult.addAll(datas)
                }
                if(response.data == null){
                    it.coinResult.username = response.errorMsg
                }
                it.copy(refreshing = false, loading = hasNextPage())
            }
        }
    }
}