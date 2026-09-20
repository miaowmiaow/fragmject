package com.example.fragmject.feature.collection.ui.myshare

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import com.example.fragmject.core.ui.contract.CollectActionHolder
import com.example.fragmject.core.domain.repository.MyRepository
import com.example.fragmject.core.domain.usecase.CollectArticleUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MyShareViewModel @Inject constructor(
    private val repo: MyRepository,
    private val collectArticle: CollectArticleUseCase,
) : ViewModel(), CollectActionHolder {

    val pagingFlow = repo.getMySharePagingData()
        .cachedIn(viewModelScope)

    override val collectAction: suspend (String, Boolean) -> Unit = { id, collect ->
        collectArticle(id, collect)
    }
}