package com.example.fragmject.feature.user.ui.user

import androidx.lifecycle.viewModelScope
import com.example.fragmject.core.model.Article
import com.example.fragmject.core.model.Coin
import com.example.fragmject.core.domain.usecase.CollectArticleUseCase
import com.example.fragmject.core.domain.usecase.GetUserShareArticlesUseCase
import com.example.fragmject.core.common.viewmodel.BaseViewModel
import com.example.fragmject.core.common.viewmodel.updateSuccessFrom
import com.example.fragmject.core.common.CollectActionHolder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface UserUiState {
    data class Success(
        val isRefreshing: Boolean = false,
        val isLoading: Boolean = false,
        val isFinishing: Boolean = false,
        val coinResult: Coin = Coin(),
        val articleResult: List<Article> = emptyList(),
    ) : UserUiState
}

val UserUiState.isRefreshing get() = (this as? UserUiState.Success)?.isRefreshing ?: false
val UserUiState.isLoading get() = (this as? UserUiState.Success)?.isLoading ?: false
val UserUiState.isFinishing get() = (this as? UserUiState.Success)?.isFinishing ?: false
val UserUiState.coinResult get() = (this as? UserUiState.Success)?.coinResult ?: Coin()
val UserUiState.articleResult get() = (this as? UserUiState.Success)?.articleResult ?: emptyList()

@HiltViewModel
class UserViewModel @Inject constructor(
    private val getUserShareArticles: GetUserShareArticlesUseCase,
    private val collectArticle: CollectArticleUseCase,
) : BaseViewModel(), CollectActionHolder {

    private var id: String = ""

    private val _uiState = MutableStateFlow<UserUiState>(UserUiState.Success())
    val uiState: StateFlow<UserUiState> = _uiState.asStateFlow()

    fun init(userId: String) {
        if (id == userId && userId.isNotBlank()) return
        id = userId
        // 清空旧数据放到 getHome 的协程里处理
        viewModelScope.launch {
            _uiState.updateSuccessFrom({ UserUiState.Success() }) {
                it.copy(
                    isRefreshing = true,
                    isLoading = false,
                    isFinishing = false,
                    coinResult = Coin(),
                    articleResult = emptyList(),
                )
            }
        }
        getHome(false)
    }

    fun getHome(userTriggered: Boolean = false) {
        if (id.isBlank()) return
        viewModelScope.launch {
            _uiState.updateSuccessFrom({ UserUiState.Success() }) {
                it.copy(
                    isRefreshing = userTriggered,
                    isLoading = false,
                    isFinishing = false
                )
            }
        }
        getList(getHomePage(1))
    }

    fun getNext() {
        _uiState.updateSuccessFrom({ UserUiState.Success() }) { it.copy(isLoading = false) }
        getList(getNextPage())
    }

    /**
     * 获取用户分享文章
     * page 1开始
     */
    private fun getList(page: Int) {
        viewModelScope.launch {
            val result = getUserShareArticles(id, page)
            val datas = result.articles
            // 数据为空 → 已到最后一页
            if (datas.isEmpty() && !isHomePage()) {
                _uiState.updateSuccessFrom({ UserUiState.Success() }) { it.copy(isLoading = false, isFinishing = true) }
                return@launch
            }
            val paging = finishPage(result.pageCount)
            _uiState.updateSuccessFrom({ UserUiState.Success() }) { state ->
                val merged = if (isHomePage()) datas else state.articleResult + datas
                state.copy(
                    isRefreshing = false,
                    isLoading = paging.isLoading,
                    isFinishing = paging.isFinishing,
                    coinResult = result.coin ?: state.coinResult,
                    articleResult = merged,
                )
            }
        }
    }

    /** 收藏 / 取消收藏。 */
    override val collectAction: suspend (String, Boolean) -> Unit = { id, collect ->
        collectArticle(id, collect)
    }
}