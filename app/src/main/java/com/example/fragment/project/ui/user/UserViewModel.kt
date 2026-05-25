package com.example.fragment.project.ui.user

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.fragment.project.data.Article
import com.example.fragment.project.data.Coin
import com.example.fragment.project.data.repository.UserRepository
import com.example.fragment.project.data.repository.WanRepositoryProvider
import com.example.miaow.base.vm.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UserUiState(
    val isRefreshing: Boolean = false,
    val isLoading: Boolean = false,
    val isFinishing: Boolean = false,
    val coinResult: Coin = Coin(),
    val articleResult: List<Article> = emptyList(),
)

class UserViewModel(
    private val id: String,
    private val userRepo: UserRepository = WanRepositoryProvider.user,
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(UserUiState())

    val uiState: StateFlow<UserUiState> = _uiState.asStateFlow()

    init {
        getHome()
    }

    fun getHome() {
        _uiState.update {
            it.copy(isRefreshing = true, isLoading = false, isFinishing = false)
        }
        getList(getHomePage(1))
    }

    fun getNext() {
        _uiState.update {
            it.copy(isRefreshing = false, isLoading = false, isFinishing = false)
        }
        getList(getNextPage())
    }

    /**
     * 获取用户分享文章
     * page 1开始
     */
    private fun getList(page: Int) {
        viewModelScope.launch {
            val response = userRepo.getUserShareArticles(id, page)
            updatePageCont(response.data?.shareArticles?.pageCount?.toInt())
            _uiState.update { state ->
                val coin = response.data?.coinInfo
                val datas = response.data?.shareArticles?.datas.orEmpty()
                val merged = if (isHomePage()) datas else state.articleResult + datas
                state.copy(
                    isRefreshing = false,
                    isLoading = hasNextPage(),
                    isFinishing = !hasNextPage(),
                    coinResult = coin ?: state.coinResult,
                    articleResult = merged,
                )
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