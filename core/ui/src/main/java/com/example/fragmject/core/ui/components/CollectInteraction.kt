package com.example.fragmject.core.ui.components

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.fragmject.core.domain.CollectState

/**
 * 收藏交互的 UI 层状态：收集到的覆盖层 + 稳定的切换回调。
 *
 * 由 [rememberCollectInteraction] 生成，替代此前散落在 Home/Search/User/System/Project/
 * PagingArticleListPage 六处重复的「overrides 收集 + onToggleClick 生成 + collectFailed Toast」样板。
 */
class CollectInteraction(
    val overrides: Map<String, Boolean>,
    val onToggleClick: (String, Boolean) -> Unit,
)

/**
 * 记住收藏交互：收集 [CollectState.overrides]、生成稳定 [CollectInteraction.onToggleClick]、
 * 并统一收集 [CollectState.collectFailed] 弹 Toast。
 */
@Composable
fun rememberCollectInteraction(collectState: CollectState): CollectInteraction {
    val overrides by collectState.overrides.collectAsStateWithLifecycle()
    // 稳定回调：每次组合生成的回调引用都是新实例，会让 FeedCard 无法跳过重组
    val onToggleClick = remember(collectState) { { id: String, collect: Boolean -> collectState.toggle(id, collect) } }
    val context = LocalContext.current
    LaunchedEffect(collectState) {
        collectState.collectFailed.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }
    return CollectInteraction(overrides = overrides, onToggleClick = onToggleClick)
}
