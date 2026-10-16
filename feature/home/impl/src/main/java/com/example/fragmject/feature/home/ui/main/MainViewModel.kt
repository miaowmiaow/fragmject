package com.example.fragmject.feature.home.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fragmject.core.domain.repository.SearchRepository
import com.example.fragmject.core.model.HotKey
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 热搜词（搜索栏轮播）ViewModel。
 *
 * 热词是首页非关键装饰性数据：Room 唯一数据源 + 网络刷新，刷新失败静默降级为缓存/空，
 * 不进入错误态、不打断首页交互（符合 dependencies-policy.md「失败可降级为无数据」约定）。
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val searchRepository: SearchRepository,
) : ViewModel() {

    private val _hotKeys = MutableStateFlow<List<HotKey>>(emptyList())
    val hotKeys: StateFlow<List<HotKey>> = _hotKeys.asStateFlow()

    init {
        viewModelScope.launch {
            searchRepository.observeHotKey().collect { _hotKeys.value = it }
        }
        viewModelScope.launch {
            searchRepository.refreshHotKey()
        }
    }
}
