package com.example.fragmject.core.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

/**
 * 首屏内容状态容器：统一承载加载（骨架屏）/空/错误三态 + 内容态。
 *
 * 数据源无关，调用方只需提供 [ContentPhase]；Paging 场景可用 `contentPhase()` 扩展从
 * `LazyPagingItems` 推导。modifier 仅作用于 loading/empty/error 分支，content 分支由调用方
 * 通过自身 modifier 控制（列表自带尺寸语义，避免重复包裹）。
 */
@Composable
fun ContentStateBox(
    phase: ContentPhase,
    modifier: Modifier = Modifier,
    minShowMillis: Int = 0,
    onRetry: () -> Unit = {},
    loadingContent: @Composable (Modifier) -> Unit = { SkeletonList(it) },
    emptyContent: @Composable (Modifier) -> Unit = { EmptyContent(it) },
    errorContent: @Composable (Modifier) -> Unit = { ErrorContent(it, onClick = onRetry) },
    content: @Composable () -> Unit,
) {
    // 进入 entry 后至少展示骨架 minShowMillis，覆盖转场动画。
    // 用 rememberSaveable：由 NavDisplay 的 entry 级 SaveableStateHolder 保留，
    // 从子页面（如 webview）返回时不重复展示；entry 出栈后清除，下次进入重新展示。
    var entrySkeletonDone by rememberSaveable { mutableStateOf(minShowMillis <= 0) }
    LaunchedEffect(Unit) {
        if (!entrySkeletonDone) {
            if (minShowMillis > 0) delay(minShowMillis.toLong().milliseconds)
            entrySkeletonDone = true
        }
    }
    when {
        !entrySkeletonDone -> loadingContent(modifier)
        phase == ContentPhase.Loading -> loadingContent(modifier)
        phase == ContentPhase.Empty -> emptyContent(modifier)
        phase == ContentPhase.Error -> errorContent(modifier)
        else -> content()
    }
}
