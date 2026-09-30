package com.example.fragmject.feature.collection.ui.myshare

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import com.example.fragmject.core.domain.CollectState
import com.example.fragmject.core.domain.repository.MyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MyShareViewModel @Inject constructor(
    private val repo: MyRepository,
    val collectState: CollectState,
) : ViewModel() {

    val pagingFlow = repo.getMySharePagingData()
        .cachedIn(viewModelScope)

}