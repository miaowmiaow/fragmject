package com.example.fragmject.feature.collection.ui.mycollection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import com.example.fragmject.core.domain.CollectState
import com.example.fragmject.core.domain.repository.MyCollectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MyCollectViewModel @Inject constructor(
    private val repo: MyCollectRepository,
    val collectState: CollectState,
) : ViewModel() {

    val pagingFlow = repo.getMyCollectPagingData()
        .cachedIn(viewModelScope)

}