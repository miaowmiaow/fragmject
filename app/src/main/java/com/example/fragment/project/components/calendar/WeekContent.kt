package com.example.fragment.project.components.calendar

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.TweenSpec
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.fragment.project.R

@Composable
internal fun WeekContent(
    items: MutableList<MutableList<CalendarDate>>,
    selectedWeek: Int,
    weekModeHeight: Float,
    monthModeHeight: Float,
    monthFillModeHeight: Float,
    isMonthFillMode: Boolean,
    offsetProvider: () -> Float,
    itemContent: @Composable LazyGridItemScope.(item: CalendarDate) -> Unit
) {
    val density = LocalDensity.current
    items.forEachIndexed { index, week ->
        var zIndex = 0f
        var weekHeight = monthFillModeHeight / items.size
        val offsetY: Float
        if (offsetProvider() <= monthModeHeight) {
            weekHeight = weekModeHeight
            val offset = weekModeHeight * index + offsetProvider() - monthModeHeight
            if (selectedWeek == index && offset < 0) {
                zIndex = 1f
                offsetY = 0f
            } else {
                zIndex = 0f
                offsetY = offset
            }
        } else {
            offsetY = offsetProvider() / items.size * index
        }
        val animatedColor by animateColorAsState(
            colorResource(if (isMonthFillMode) R.color.white else R.color.background),
            animationSpec = TweenSpec(350),
            label = "color"
        )
        LazyVerticalGrid(
            columns = GridCells.Fixed(DAYS_IN_WEEK),
            modifier = Modifier
                .fillMaxWidth()
                .requiredHeight(with(density) { weekHeight.toDp() })
                .offset { IntOffset(x = 0, y = offsetY.toInt()) }
                .zIndex(zIndex)
                .then(if (isMonthFillMode) Modifier.padding(bottom = 1.dp) else Modifier)
                .background(animatedColor),
            userScrollEnabled = false
        ) {
            items(week) {
                itemContent(it)
            }
        }
    }
}