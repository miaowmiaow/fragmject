package com.example.fragmject.feature.auth.components

import androidx.compose.runtime.Immutable

/** 表单的文案与加载态；登录/注册共用同一结构，差异只在取值。 */
@Immutable
data class AccountFormTexts(
    val title: String,
    val subtitle: String,
    val buttonText: String,
    val bottomLinkText: String,
    val isLoading: Boolean,
)
