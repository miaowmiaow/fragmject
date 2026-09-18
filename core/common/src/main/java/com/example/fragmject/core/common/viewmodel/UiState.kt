package com.example.fragmject.core.common.viewmodel

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/**
 * 更新 Success 状态：当前已是 Success 时直接执行 block，
 * 否则（如 Loading）先通过 fallback 构造默认 Success 再执行 block。
 * 类型 A（仅 Success 一个子类）与类型 B（含 Loading 等分支）统一使用本函数。
 */
inline fun <reified S, T> MutableStateFlow<T>.updateSuccessFrom(
    crossinline fallback: () -> S,
    crossinline block: (S) -> S,
) where S : T {
    update { block(if (it is S) it else fallback()) }
}
