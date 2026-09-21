package com.example.fragmject.feature.user.ui.user

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import com.example.fragmject.core.domain.repository.UserCenterRepository
import com.example.fragmject.core.domain.usecase.CollectArticleUseCase
import com.example.fragmject.core.model.Coin
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class UserViewModel @Inject constructor(
    private val repo: UserCenterRepository,
    private val collectArticle: CollectArticleUseCase,
) : ViewModel() {

    private val _userId = MutableStateFlow("")

    private val _coin = MutableStateFlow(Coin())
    val coin: StateFlow<Coin> = _coin.asStateFlow()

    /** 用户分享文章分页流，随 userId 变化重建。 */
    val pagingFlow = _userId
        .filter { it.isNotBlank() }
        .flatMapLatest { userId -> repo.getUserSharePagingData(userId) }
        .cachedIn(viewModelScope)

    fun init(userId: String) {
        if (_userId.value == userId && userId.isNotBlank()) return
        _userId.value = userId
        loadCoin(userId)
    }

    private fun loadCoin(userId: String) {
        viewModelScope.launch {
            _coin.value = repo.getUserCoin(userId) ?: Coin()
        }
    }

    /** 收藏 / 取消收藏。 */
    suspend fun collect(id: String, collect: Boolean) {
        collectArticle(id, collect)
    }
}