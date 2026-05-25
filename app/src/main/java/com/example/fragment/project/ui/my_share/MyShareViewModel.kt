package com.example.fragment.project.ui.my_share

import androidx.lifecycle.viewModelScope
import com.example.fragment.project.data.Article
import com.example.fragment.project.data.repository.MyRepository
import com.example.fragment.project.data.repository.WanRepositoryProvider
import com.example.miaow.base.vm.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MyShareUiState(
    val isRefreshing: Boolean = false,
    val isLoading: Boolean = false,
    val isFinishing: Boolean = false,
    val result: List<Article> = emptyList(),
)

class MyShareViewModel(
    private val myRepo: MyRepository = WanRepositoryProvider.my,
) : BaseViewModel() {

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
            val response = myRepo.getMyShareList(page)
            updatePageCont(response.data?.shareArticles?.pageCount?.toInt())
            _uiState.update { state ->
                val datas = response.data?.shareArticles?.datas.orEmpty()
                val merged = if (isHomePage()) datas else state.result + datas
                state.copy(
                    isRefreshing = false,
                    isLoading = hasNextPage(),
                    isFinishing = !hasNextPage(),
                    result = merged
                )
            }
        }
    }

}