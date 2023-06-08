package com.example.fragment.project.ui.share

import androidx.lifecycle.viewModelScope
import com.example.fragment.library.base.http.HttpRequest
import com.example.fragment.library.base.http.HttpResponse
import com.example.fragment.library.base.http.post
import com.example.fragment.library.base.vm.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ShareArticleState(
    var isLoading: Boolean = false,
    var result: HttpResponse = HttpResponse(),
)

class ShareArticleViewModel : BaseViewModel() {

    private val _uiState = MutableStateFlow(ShareArticleState())

    val uiState: StateFlow<ShareArticleState> = _uiState.asStateFlow()

    fun share(title: String, link: String) {
        _uiState.update {
            it.copy(isLoading = true)
        }
        //通过viewModelScope创建一个协程
        viewModelScope.launch {
            //构建请求体，传入请求参数
            val request = HttpRequest("lg/user_article/add/json").putParam("title", title).putParam("link", link)
            //以get方式发起网络请求
            val response = post<HttpResponse>(request)
            _uiState.update {
                it.copy(isLoading = false, result = response)
            }
        }
    }
}