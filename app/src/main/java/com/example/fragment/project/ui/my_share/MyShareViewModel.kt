package com.example.fragment.project.ui.my_share

import androidx.lifecycle.viewModelScope
import com.example.fragment.library.base.http.HttpRequest
import com.example.fragment.library.base.http.get
import com.example.fragment.library.base.vm.BaseViewModel
import com.example.fragment.project.bean.ArticleBean
import com.example.fragment.project.bean.ShareArticleListBean
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MyShareState(
    var refreshing: Boolean = false,
    var loading: Boolean = false,
    var result: MutableList<ArticleBean> = ArrayList(),
)

class MyShareViewModel : BaseViewModel() {

    private val _uiState = MutableStateFlow(MyShareState())

    val uiState: StateFlow<MyShareState> = _uiState.asStateFlow()

    init {
        getHome()
    }

    fun getHome() {
        _uiState.update { it.copy(refreshing = true) }
        getList(getHomePage())
    }

    fun getNext() {
        _uiState.update { it.copy(loading = false) }
        getList(getNextPage())
    }


    /**
     * 获取自己的分享的文章
     * page 1开始
     */
    private fun getList(page: Int) {
        //通过viewModelScope创建一个协程
        viewModelScope.launch {
            //构建请求体，传入请求参数
            val request = HttpRequest("user/lg/private_articles/{page}/json")
                .putPath("page", page.toString())
            //以get方式发起网络请求
            val response = get<ShareArticleListBean>(request) { updateProgress(it) }
            //根据接口返回更新总页码
            response.data?.shareArticles?.pageCount?.let { updatePageCont(it.toInt()) }
            _uiState.update {
                response.data?.shareArticles?.datas?.let { datas ->
                    if (isHomePage()) {
                        it.result.clear()
                    }
                    it.result.addAll(datas)
                }
                it.copy(refreshing = false, loading = hasNextPage())
            }
        }
    }

}