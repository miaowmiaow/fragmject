package com.example.fragmject.feature.home.ui.home

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.View
import android.view.ViewTreeObserver
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import com.example.fragmject.core.designsystem.AppTheme
import com.example.fragmject.core.navigation.contract.LocalNavigators
import com.example.fragmject.core.ui.components.ContentPhase
import com.example.fragmject.core.ui.components.contentPhase
import com.example.fragmject.core.ui.components.rememberCollectInteraction
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

@Composable
fun HomeScreen(
    listState: LazyListState,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val navigators = LocalNavigators.current
    val onArticleClick = remember(navigators) { { url: String -> navigators.article.openArticle(url) } }
    val onAuthorClick = remember(navigators) { { userId: String -> navigators.user.openUserProfile(userId) } }
    val onChapterClick = remember(navigators) { { cid: String -> navigators.home.openSystemTree(cid) } }
    // 点封面开大图预览：网络图，纯看图（不可编辑）
    val onCoverClick = remember(navigators) {
        { url: String -> navigators.picture.openPicturePreview(listOf(url)) }
    }
    val banners by viewModel.banners.collectAsStateWithLifecycle()
    val topArticles by viewModel.topArticles.collectAsStateWithLifecycle()
    val pagingItems = viewModel.pagingFlow.collectAsLazyPagingItems()
    val collectInteraction = rememberCollectInteraction(viewModel.collectState)
    val headerError by viewModel.headerError.collectAsStateWithLifecycle()
    // 只推导一次：contentPhase() 会读 LoadState 与 itemCount，放在参数位置会在每次重组时重复计算。
    val phase = pagingItems.contentPhase()
    // 首屏「可用」时刻：骨架屏（Loading）不算，空态/错误态/内容态都视为首屏已绘制完成。
    // 上报后 Macrobenchmark 才有 timeToFullDisplay，否则首页优化的收益无法量化对比。
    // 依赖前提：contentPhase() 在首帧（尚未开始加载）返回 Loading 而不是 Empty，
    // 否则上报会发生在「空态」上，timeToFullDisplay 量的就不是内容可用时刻。
    ReportFullyDrawnWhen(phase != ContentPhase.Loading)
    HomeFeedContent(
        pagingItems = pagingItems,
        phase = phase,
        spec = HomeFeedSpec(
            banners = banners,
            topArticles = topArticles,
            hasHeaderError = headerError != null,
            listState = listState,
            collectInteraction = collectInteraction,
        ),
        actions = HomeFeedActions(
            onArticleClick = onArticleClick,
            onAuthorClick = onAuthorClick,
            onChapterClick = onChapterClick,
            onCoverClick = onCoverClick,
            onRetry = { pagingItems.retry() },
            onRefreshHeader = viewModel::loadHeader,
        ),
    )
}

@Preview(showBackground = true, backgroundColor = 0xFFF0F0F0)
@Composable
fun HomeScreenPreview() {
    AppTheme { HomeScreen(rememberLazyListState()) }
}

/**
 * 首屏内容「用户可用」时调用 [Activity.reportFullyDrawn]。
 *
 * 为什么不用 `androidx.activity.compose.ReportDrawnWhen`：它在拿不到 Activity 的宿主
 * （例如 `@Preview` 的 BridgeContext）会直接抛异常；这里找不到 Activity 就静默跳过。
 *
 * 骨架屏（[ContentPhase.Loading]）期间不上报，因此 Macrobenchmark 得到的
 * `timeToFullDisplay` 与 `timeToInitialDisplay` 之差，就是骨架屏的实际停留时长 ——
 * 这是首页体验优化唯一可信的对比基准。
 */
@Composable
private fun ReportFullyDrawnWhen(ready: Boolean) {
    val view = LocalView.current
    LaunchedEffect(ready) {
        // 进程内只上报一次：温启动复用同一进程，内容在 launch 之前就已绘制完成，
        // 此时上报会让宏基准找不到与之对应的 RenderThread 帧，直接抛
        // "No RT frame slice associated with UI thread frame slice ends after reportFullyDrawn"
        // （模拟器 warmStartup 必现）。冷启动每轮都是新进程，因此仍能正常上报。
        if (!ready || ProcessFullyDrawn.reported) return@LaunchedEffect
        // 必须等到「真正发生一次绘制」再上报。
        //
        // 只等帧回调（withFrameNanos）并不够：帧回调存在不代表该帧有渲染输出，没有绘制就不会
        // 产生 RenderThread frame slice，同样会触发上面的异常。
        //
        // 也不能等两帧以上：上报越晚越容易落在 launching 窗口之外，该轮会被
        // "didn't capture all metrics" 丢弃（真机实测捕获率 2/5 → 1/5）。
        awaitContentDrawn(view)
        ProcessFullyDrawn.reported = true
        view.context.findActivityOrNull()?.reportFullyDrawn()
    }
}

/** 进程级标记：整个进程只向系统上报一次 fully drawn。 */
private object ProcessFullyDrawn {
    @Volatile
    var reported = false
}

/**
 * 挂起直到 [view] 完成一次真实绘制并把该帧提交给 RenderThread。
 *
 * [ViewTreeObserver.OnDrawListener] 在绘制流程开始处回调，此时帧尚未提交；因此移除监听与
 * 恢复协程都放到 [View.post]（当前帧结束后的下一个消息），确保上报时 RT slice 已生成。
 * 注意不能在 `onDraw()` 内直接 `removeOnDrawListener`，系统会抛
 * "Cannot call removeOnDrawListener during draw"。
 */
private suspend fun awaitContentDrawn(view: View) = suspendCancellableCoroutine { cont ->
    val observer = view.viewTreeObserver
    val listener = object : ViewTreeObserver.OnDrawListener {
        override fun onDraw() {
            view.post {
                if (observer.isAlive) observer.removeOnDrawListener(this)
                if (cont.isActive) cont.resume(Unit)
            }
        }
    }
    observer.addOnDrawListener(listener)
    cont.invokeOnCancellation {
        view.post { if (observer.isAlive) observer.removeOnDrawListener(listener) }
    }
}

private tailrec fun Context.findActivityOrNull(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivityOrNull()
    else -> null
}
