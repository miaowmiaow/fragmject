package com.example.fragment.library.base.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import coil.compose.AsyncImage
import com.example.fragment.library.base.R
import com.google.accompanist.pager.*
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.*
import kotlin.math.absoluteValue

@OptIn(ExperimentalPagerApi::class)
@Composable
fun <T> Banner(
    modifier: Modifier = Modifier,
    data: List<T>?,
    pathMapping: (T) -> String,
    onClick: (index: Int, item: T) -> Unit
) {
    data?.let {
        val pageCount = it.size
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
                contentPadding = PaddingValues(horizontal = 32.dp),
            ) { page ->
                Card(
                    Modifier
                        .graphicsLayer {
                            val pageOffset = calculateCurrentOffsetForPage(page).absoluteValue
                            lerp(
                                start = 0.9f,
                                stop = 1f,
                                fraction = 1f - pageOffset.coerceIn(0f, 1f)
                            ).also { scale ->
                                scaleX = scale
                                scaleY = scale
                            }
                        }
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                ) {
                    val index = (page - startIndex).floorMod(pageCount)
                    AsyncImage(
                        model = pathMapping(it[index]),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(16f))
                            .clickable {
                                onClick(index, it[index])
                            }
                    )
                }
            }
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

private fun Int.floorMod(other: Int): Int = when (other) {
    0 -> this
    else -> this - floorDiv(other) * other
}