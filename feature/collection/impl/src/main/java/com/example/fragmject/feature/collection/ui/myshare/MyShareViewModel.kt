package com.example.fragmject.feature.collection.ui.myshare

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import com.example.fragmject.core.domain.repository.MyRepository
import com.example.fragmject.core.domain.usecase.CollectArticleUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MyShareViewModel @Inject constructor(
    private val repo: MyRepository,
    private val collectArticle: CollectArticleUseCase,
) : ViewModel() {

    val pagingFlow = repo.getMySharePagingData()
        .cachedIn(viewModelScope)

    suspend fun collect(id: String, collect: Boolean) {
        collectArticle(id, collect)
    }
}