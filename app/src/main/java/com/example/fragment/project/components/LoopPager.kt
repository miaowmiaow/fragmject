package com.example.fragment.project.components

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import com.example.fragment.library.base.R
import com.google.accompanist.pager.*
import kotlinx.coroutines.launch
import java.util.*
import kotlin.math.absoluteValue

@OptIn(ExperimentalPagerApi::class)
@Composable
fun <T> LoopHorizontalPager(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    data: List<T>?,
    indicator: Boolean = false,
    content: @Composable PagerScope.(page: Int, pageOffset: Float, item: T) -> Unit,
) {
    if (data.isNullOrEmpty()) {
        return
    }
    val pageCount = data.size
    val startIndex = Int.MAX_VALUE / 2
    val pagerState = rememberPagerState(initialPage = startIndex)
    val coroutineScope = rememberCoroutineScope()
    DisposableEffect(Unit) {
        val timer = Timer()
        timer.schedule(object : TimerTask() {
            override fun run() {
                coroutineScope.launch {
                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                }
            }
        }, 3000, 3000)
        onDispose {
            timer.cancel()
        }
    }
    Box(
        modifier = modifier,
        contentAlignment = Alignment.BottomCenter
    ) {
        HorizontalPager(
            count = Int.MAX_VALUE,
            state = pagerState,
            contentPadding = contentPadding,
        ) { page ->
            val currPage = (page - startIndex).floorMod(pageCount)
            val pageOffset = calculateCurrentOffsetForPage(page).absoluteValue
            content(currPage, pageOffset, data[currPage])
        }
        if (indicator) {
            HorizontalPagerIndicator(
                pagerState = pagerState,
                modifier = Modifier.paddingFromBaseline(bottom = 15.dp),
                pageCount = pageCount,
                pageIndexMapping = { page ->
                    (page - startIndex).floorMod(pageCount)
                },
                activeColor = colorResource(R.color.orange),
                inactiveColor = colorResource(R.color.theme)
            )
        }
    }
}

@OptIn(ExperimentalPagerApi::class)
@Composable
fun <T> LoopVerticalPager(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    data: List<T>?,
    indicator: Boolean = false,
    content: @Composable PagerScope.(page: Int, pageOffset: Float, item: T) -> Unit,
) {
    if (data.isNullOrEmpty()) {
        return
    }
    val pageCount = data.size
    val startIndex = Int.MAX_VALUE / 2
    val pagerState = rememberPagerState(initialPage = startIndex)
    val coroutineScope = rememberCoroutineScope()
    DisposableEffect(Unit) {
        val timer = Timer()
        timer.schedule(object : TimerTask() {
            override fun run() {
                coroutineScope.launch {
                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                }
            }
        }, 3000, 3000)
        onDispose {
            timer.cancel()
        }
    }
    Box(
        modifier = modifier,
        contentAlignment = Alignment.BottomCenter
    ) {
        VerticalPager(
            count = Int.MAX_VALUE,
            state = pagerState,
            contentPadding = contentPadding,
        ) { page ->
            val currPage = (page - startIndex).floorMod(pageCount)
            val pageOffset = calculateCurrentOffsetForPage(page).absoluteValue
            content(currPage, pageOffset, data[currPage])
        }
        if (indicator) {
            VerticalPagerIndicator(
                pagerState = pagerState,
                modifier = Modifier.paddingFromBaseline(bottom = 15.dp),
                pageCount = pageCount,
                pageIndexMapping = { page ->
                    (page - startIndex).floorMod(pageCount)
                },
                activeColor = colorResource(R.color.orange),
                inactiveColor = colorResource(R.color.theme)
            )
        }
    }
}

private fun Int.floorMod(other: Int): Int = when (other) {
    0 -> this
    else -> this - floorDiv(other) * other
}