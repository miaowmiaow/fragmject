package com.example.fragmject.feature.collection.ui.mycollection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import com.example.fragmject.core.ui.contract.CollectActionHolder
import com.example.fragmject.core.domain.repository.MyCollectRepository
import com.example.fragmject.core.domain.usecase.CollectArticleUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MyCollectViewModel @Inject constructor(
    private val repo: MyCollectRepository,
    private val collectArticle: CollectArticleUseCase,
) : ViewModel(), CollectActionHolder {

    val pagingFlow = repo.getMyCollectPagingData()
        .cachedIn(viewModelScope)

    override val collectAction: suspend (String, Boolean) -> Unit = { id, collect ->
        collectArticle(id, collect)
    }
}