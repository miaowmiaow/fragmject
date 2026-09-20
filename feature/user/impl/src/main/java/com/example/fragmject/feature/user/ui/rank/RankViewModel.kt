package com.example.fragmject.feature.user.ui.rank

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import com.example.fragmject.core.domain.repository.CoinRankRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class RankViewModel @Inject constructor(
    private val repo: CoinRankRepository,
) : ViewModel() {

    val pagingFlow = repo.getCoinRankPagingData()
        .cachedIn(viewModelScope)
}