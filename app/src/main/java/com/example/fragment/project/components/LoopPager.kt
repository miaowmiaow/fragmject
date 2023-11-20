package com.example.fragment.project.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import com.example.fragment.project.R
import kotlinx.coroutines.launch
import java.util.Timer
import java.util.TimerTask
import kotlin.math.absoluteValue

@OptIn(ExperimentalFoundationApi::class)
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
    val size = pageCount * 10000
    val startIndex = size / 2
    val pagerState = rememberPagerState(startIndex - startIndex % pageCount) { size }
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
            state = pagerState,
            contentPadding = contentPadding,
        ) { page ->
            val currPage = (page - startIndex).floorMod(pageCount)
            content(
                currPage,
                ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction).absoluteValue,
                data[currPage]
            )
        }
        if (indicator) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 5.dp)
                    .align(Alignment.BottomCenter),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(pageCount) { iteration ->
                    val currentPage = (pagerState.currentPage - startIndex).floorMod(pageCount)
                    val color = if (currentPage == iteration)
                        colorResource(R.color.orange)
                    else
                        colorResource(R.color.theme)
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .padding(2.dp)
                            .clip(CircleShape)
                            .background(color)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
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
    val size = pageCount * 10000
    val startIndex = size / 2
    val pagerState = rememberPagerState(startIndex - startIndex % pageCount) { size }
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
            state = pagerState,
            contentPadding = contentPadding,
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
                verticalArrangement = Arrangement.Center
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
                            .clip(CircleShape)
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