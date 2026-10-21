package com.example.fragmject.feature.picture.ui.editor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * 底部工具栏的本地 UI 状态：当前选中工具、颜色栏与马赛克撤销入口的显隐。
 *
 * 单独成类是为了让 [EditorToolbar] 自己管理展开态，同时保留「文字/裁剪完成后
 * 取消选中」这一跨组件重置能力（[resetSelection]）。
 */
@Stable
internal class EditorToolbarController {
    var selectedToolIndex by mutableIntStateOf(-1)
    var showColorBar by mutableStateOf(false)
    var showMosaicUndo by mutableStateOf(false)

    /** 文字/裁剪等子流程结束后，取消工具高亮并收起附加栏。 */
    fun resetSelection() {
        selectedToolIndex = -1
        showColorBar = false
        showMosaicUndo = false
    }
}

@Composable
internal fun rememberEditorToolbarController(): EditorToolbarController =
    remember { EditorToolbarController() }
