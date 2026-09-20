package com.example.fragmject.feature.user.ui.mycoin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import com.example.fragmject.core.domain.repository.MyRepository
import com.example.fragmject.core.model.Coin
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MyCoinViewModel @Inject constructor(
    private val repo: MyRepository,
) : ViewModel() {

    private val _coin = MutableStateFlow(Coin())
    val coin: StateFlow<Coin> = _coin.asStateFlow()

    /** 积分明细分页数据流，cachedIn 保证配置变更后复用已加载数据。 */
    val pagingFlow = repo.getMyCoinPagingData()
        .cachedIn(viewModelScope)

    init { loadCoin() }

    private fun loadCoin() {
        viewModelScope.launch {
            _coin.value = repo.getUserCoin() ?: Coin()
        }
    }
}