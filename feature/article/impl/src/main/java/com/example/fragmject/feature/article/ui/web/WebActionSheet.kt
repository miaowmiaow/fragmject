package com.example.fragmject.feature.article.ui.web

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.fragmject.core.model.History
import com.example.fragmject.core.ui.R

/**
 * WebView 底部操作面板：单行常用操作（刷新 / 历史 / 收藏 / 调试注入）。
 *
 * 仅负责渲染，全部动作由 [WebScreen] 通过回调注入（含 partialExpand 等编排），
 * 保持无状态、单向数据流。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebActionSheet(
    bottomSheetState: SheetState,
    bookmark: History?,
    injectState: Boolean,
    onReload: () -> Unit,
    onOpenHistory: () -> Unit,
    onToggleBookmark: () -> Unit,
    onToggleInject: () -> Unit,
    content: @Composable (PaddingValues) -> Unit,
) {
    val scaffoldState = rememberBottomSheetScaffoldState(bottomSheetState)
    BottomSheetScaffold(
        sheetContent = {
            Row(modifier = Modifier.height(64.dp)) {
                WebActionButton(
                    onClick = onReload,
                    icon = {
                        Icon(
                            painter = painterResource(R.mipmap.ic_web_refresh),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    modifier = Modifier.weight(1f)
                )
                WebActionButton(
                    onClick = onOpenHistory,
                    icon = {
                        Icon(
                            painter = painterResource(R.mipmap.ic_web_history),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    modifier = Modifier.weight(1f)
                )
                WebActionButton(
                    onClick = onToggleBookmark,
                    icon = {
                        Icon(
                            painter = painterResource(R.mipmap.ic_web_bookmark),
                            contentDescription = null,
                            tint = if (bookmark != null) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    },
                    modifier = Modifier.weight(1f)
                )
                WebActionButton(
                    onClick = onToggleInject,
                    icon = {
                        Icon(
                            painter = painterResource(R.mipmap.ic_web_debug),
                            contentDescription = null,
                            tint = if (injectState) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        },
        scaffoldState = scaffoldState,
        sheetPeekHeight = 0.dp,
        sheetShape = RoundedCornerShape(0.dp),
        sheetShadowElevation = if (
            bottomSheetState.currentValue == SheetValue.Expanded ||
            bottomSheetState.targetValue == SheetValue.Expanded
        ) {
            10.dp
        } else {
            0.dp
        },
        sheetDragHandle = null,
        sheetSwipeEnabled = false
    ) { padding ->
        content(padding)
    }
}
