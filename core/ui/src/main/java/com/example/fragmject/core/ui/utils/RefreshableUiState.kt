package com.example.fragmject.core.ui.utils

import com.example.fragmject.core.domain.result.DomainResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 可刷新列表的统一三态状态模型。
 *
 * 收敛此前并存的三种状态建模（Success 内嵌 isLoading 二态、独立 Loading 三态、多 StateFlow），
 * 统一为 Loading / Success / Error，供「Room 唯一数据源 + 网络刷新」类 ViewModel 复用。
 */
sealed interface RefreshableUiState<out T> {
    data object Loading : RefreshableUiState<Nothing>
    data class Success<T>(val data: T) : RefreshableUiState<T>
    data class Error(val message: String) : RefreshableUiState<Nothing>
}

/** 取数据：Success 返回 data，Loading / Error 返回空列表（空态/错误态由 UI 层用 when 判定）。 */
fun <T> RefreshableUiState<List<T>>.dataOrEmpty(): List<T> =
    (this as? RefreshableUiState.Success)?.data.orEmpty()

/**
 * 统一「observe(本地缓存) + refresh(网络)」编排，含错误态优先：
 * - observe 流发射时，若当前为 Error 则跳过（错误态优先，直到显式 retry），否则转 Success；
 * - refresh 失败转 Error；成功不直接写数据，依赖 refresh 回写 Room 触发 observe 流重发。
 */
fun <T> CoroutineScope.collectAndRefresh(
    observe: Flow<T>,
    refresh: suspend () -> DomainResult<Unit>,
    state: MutableStateFlow<RefreshableUiState<T>>,
) {
    launch {
        observe.collect { data ->
            state.update { cur ->
                cur as? RefreshableUiState.Error ?: RefreshableUiState.Success(data)
            }
        }
    }
    launch {
        when (val r = refresh()) {
            is DomainResult.Success -> Unit
            is DomainResult.Failure -> state.value = RefreshableUiState.Error(r.message)
        }
    }
}

/** 错误态重试：转 Loading 后重新刷新，成功后由 observe 流（refresh 回写 Room）转 Success。 */
fun <T> CoroutineScope.retryRefresh(
    refresh: suspend () -> DomainResult<Unit>,
    state: MutableStateFlow<RefreshableUiState<T>>,
) {
    launch {
        state.value = RefreshableUiState.Loading
        when (val r = refresh()) {
            is DomainResult.Success -> Unit
            is DomainResult.Failure -> state.value = RefreshableUiState.Error(r.message)
        }
    }
}
