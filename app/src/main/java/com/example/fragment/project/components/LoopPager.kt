package com.example.fragment.project.components

import android.annotation.SuppressLint
import androidx.compose.animation.core.TweenSpec
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import com.example.fragment.project.R
import com.example.fragment.project.WanTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.absoluteValue
import kotlin.time.Duration.Companion.milliseconds

private const val LOOP_VIRTUAL_PAGE_COUNT = 10000
private const val LOOP_AUTO_SCROLL_INTERVAL_MS = 5000L
private const val LOOP_AUTO_SCROLL_ANIMATION_MS = 1000

/**
 * 循环轮播的初始化状态。
 *
 * - [state]：以 [pageCount] 为 key 强制重建的 [PagerState]
 * - [startIndex]：虚拟页中的居中起点，用于把虚拟页号换算为真实数据下标
 *
 * [PagerState.initialPage] 在当前 androidx.compose.foundation 版本中并非可读 public API，
 * 因此这里把 startIndex 一并返回，避免调用方反向读取造成类型推断失败。
 */
private data class LoopPagerState(
    val state: PagerState,
    val startIndex: Int,
)

/**
 * 创建用于"无限循环"轮播的 [PagerState] + 起点页号。
 *
 * 之所以单独抽一个：原实现里 [rememberPagerState] 没有以 `pageCount` 作为 key，
 * 当外部 `data` 数量发生变化（例如热词接口异步返回 / 登录态切换）时，
 * 旧的 PagerState 会保留旧 `pageCount`，与新 size 不匹配，导致 `currPage` 计算错位、
 * 视觉上出现热词跳变 / 撕裂残影。这里以 [pageCount] 作 key1 强制重建。
 */
@Composable
private fun rememberLoopPagerState(pageCount: Int): LoopPagerState {
    val virtualSize = pageCount * LOOP_VIRTUAL_PAGE_COUNT
    val startIndex = remember(pageCount) { virtualSize / 2 - (virtualSize / 2) % pageCount }
    val pagerState = rememberPagerState(
        initialPage = startIndex,
        pageCount = { virtualSize },
    )
    return remember(pagerState, startIndex) { LoopPagerState(pagerState, startIndex) }
}

/**
 * 自动轮播驱动：等到上一次动画完全结束（含 [LOOP_AUTO_SCROLL_INTERVAL_MS] 间隔）后再触发下一次，
 * 避免与正在进行中的滑动 / 上一次未结束的动画产生竞态。
 *
 * 旧实现使用 [java.util.Timer] 固定周期 + 每次 `coroutineScope.launch { animateScrollToPage(...) }`，
 * 在低端机或动画时长 > 1 秒时上一帧动画未结束下一次就进来了，两次 animate 同时争抢
 * 同一 [PagerState]，表现为"热词偶尔跳变 / 撕裂"。
 */
@Composable
private fun AutoLoopScrollEffect(pagerState: PagerState, pageCount: Int) {
    LaunchedEffect(pagerState, pageCount) {
        if (pageCount <= 1) return@LaunchedEffect
        while (isActive) {
            delay(LOOP_AUTO_SCROLL_INTERVAL_MS.milliseconds)
            // 用户正在拖动 / 上一次动画未结束时，本轮跳过，避免抢占。
            if (pagerState.isScrollInProgress) continue
            pagerState.animateScrollToPage(
                page = pagerState.currentPage + 1,
                animationSpec = TweenSpec(durationMillis = LOOP_AUTO_SCROLL_ANIMATION_MS),
            )
        }
    }
}

@SuppressLint("FrequentlyChangingValue")
@Composable
fun <T> LoopHorizontalPager(
    data: List<T>?,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    indicator: Boolean = false,
    content: @Composable (page: Int, pageOffset: Float, item: T) -> Unit,
) {
    if (data.isNullOrEmpty()) {
        return
    }
    val pageCount = data.size
    val (pagerState, startIndex) = rememberLoopPagerState(pageCount)
    AutoLoopScrollEffect(pagerState = pagerState, pageCount = pageCount)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(modifier),
        contentAlignment = Alignment.BottomCenter,
    ) {
        HorizontalPager(
            state = pagerState,
            contentPadding = contentPadding,
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds(),
        ) { page ->
            val currPage = (page - startIndex).floorMod(pageCount)
            content(
                currPage,
                ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction).absoluteValue,
                data[currPage],
            )
        }
        if (indicator) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 5.dp)
                    .align(Alignment.BottomCenter),
                horizontalArrangement = Arrangement.Center,
            ) {
                repeat(pageCount) { iteration ->
                    val currentPage = (pagerState.currentPage - startIndex).floorMod(pageCount)
                    val color = if (currentPage == iteration) WanTheme.orange else WanTheme.theme
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .padding(2.dp)
                            .clip(CircleShape)
                            .background(color),
                    )
                }
            }
        }
    }
}

@Composable
fun <T> LoopVerticalPager(
    data: List<T>?,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    indicator: Boolean = false,
    content: @Composable (page: Int, pageOffset: Float, item: T) -> Unit,
) {
    if (data.isNullOrEmpty()) {
        return
    }
    val pageCount = data.size
    val (pagerState, startIndex) = rememberLoopPagerState(pageCount)
    AutoLoopScrollEffect(pagerState = pagerState, pageCount = pageCount)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(modifier),
        contentAlignment = Alignment.BottomCenter,
    ) {
        VerticalPager(
            state = pagerState,
            contentPadding = contentPadding,
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds(),
        ) { page ->
            val currPage = (page - startIndex).floorMod(pageCount)
            content(currPage, page.absoluteValue.toFloat(), data[currPage])
        }
        if (indicator) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(end = 5.dp)
                    .align(Alignment.CenterEnd),
                verticalArrangement = Arrangement.Center,
            ) {
                repeat(pageCount) { iteration ->
                    val currentPage = (pagerState.currentPage - startIndex).floorMod(pageCount)
                    val color = if (currentPage == iteration)
                        colorResource(R.color.orange)
                    else
                        colorResource(R.color.theme)
                    Box(
                        modifier = Modifier
                            .background(color)
                            .size(10.dp)
                            .padding(2.dp)
                            .clip(CircleShape),
                    )
                }
            }
        }
    }
}

private fun Int.floorMod(other: Int): Int = when (other) {
    0 -> this
    else -> this - floorDiv(other) * other
}