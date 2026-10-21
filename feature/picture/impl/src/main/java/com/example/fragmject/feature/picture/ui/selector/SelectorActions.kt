package com.example.fragmject.feature.picture.ui.selector

import androidx.compose.runtime.Stable

/**
 * 选择页对外暴露的全部交互，按用途成组传入。
 *
 * 单独成类是为了让 [SelectorContent] 的参数表保持在个位数，而不是把六个 lambda 平铺。
 */
@Stable
internal class SelectorActions(
    val onBack: () -> Unit,
    val onConfirm: () -> Unit,
    val onExpandMenu: (Boolean) -> Unit,
    val onSelectAlbum: (String) -> Unit,
    val onTakePicture: () -> Unit,
    val onToggleSelection: (String) -> Unit,
)
