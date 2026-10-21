package com.example.fragmject.feature.demo.ui.calendar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.ScrollAxisRange
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.verticalScrollAxisRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.max

@Composable
internal fun YearPicker(
    mode: CalendarMode,
    model: CalendarModel,
    weekModePagerState: PagerState,
    monthModePagerState: PagerState,
) {
    val isWeekMode = mode == CalendarMode.Week
    val weekPage = weekModePagerState.currentPage
    val monthPage = monthModePagerState.currentPage
    val year = currentYear(isWeekMode, model, weekPage, monthPage)
    val month = currentMonth(isWeekMode, model, weekPage, monthPage)
    Column {
        var visible by remember { mutableStateOf(false) }
        YearPickerHeader(year, month, visible) { visible = !visible }
        AnimatedVisibility(
            visible = visible,
            modifier = Modifier.clipToBounds(),
            enter = expandVertically() + fadeIn(initialAlpha = 0.6f),
            exit = shrinkVertically() + fadeOut()
        ) {
            YearPickerGrid(
                year = year,
                model = model,
                onYearSelected = { localYear ->
                    if (isWeekMode) {
                        weekModePagerState.scrollToPage(model.weekModeIndexByDate(localYear, month))
                    } else {
                        monthModePagerState.scrollToPage((localYear - model.startYear()) * 12 + month - 1)
                    }
                },
                onDismiss = { visible = false },
            )
        }
    }
}

/** 当前展示的年份：周模式取周 Pager 页码对应年份，月模式按页码换算。 */
private fun currentYear(
    isWeekMode: Boolean,
    model: CalendarModel,
    weekPage: Int,
    monthPage: Int,
): Int = if (isWeekMode) {
    model.yearByWeekModeIndex(weekPage)
} else {
    model.startYear() + monthPage / 12
}

/** 当前展示的月份：周模式取周 Pager 页码对应月份，月模式按页码换算。 */
private fun currentMonth(
    isWeekMode: Boolean,
    model: CalendarModel,
    weekPage: Int,
    monthPage: Int,
): Int = if (isWeekMode) model.monthByWeekModeIndex(weekPage) else monthPage % 12 + 1

/** 顶部“xxxx年xx月”标题与展开箭头。 */
@Composable
private fun YearPickerHeader(
    year: Int,
    month: Int,
    visible: Boolean,
    onToggle: () -> Unit,
) {
    TextButton(
        onClick = onToggle,
        modifier = Modifier.height(YearHeight),
        shape = CircleShape,
    ) {
        Text(
            text = "${year}年${month}月",
            color = MaterialTheme.colorScheme.onPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
        Icon(
            Icons.Filled.ArrowDropDown,
            contentDescription = "",
            Modifier.rotate(if (visible) 180f else 0f),
            tint = MaterialTheme.colorScheme.onPrimary
        )
    }
}

/** 展开后的年份网格；点击某年滚动到该年同名月份并收起。 */
@Composable
private fun YearPickerGrid(
    year: Int,
    model: CalendarModel,
    onYearSelected: suspend (year: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val lazyGridState = rememberLazyGridState(
        initialFirstVisibleItemIndex = max(0, year - model.startYear() - YEAR_IN_ROW)
    )
    LazyVerticalGrid(
        columns = GridCells.Fixed(YEAR_IN_ROW),
        modifier = Modifier
            .background(MaterialTheme.colorScheme.background)
            .semantics {
                verticalScrollAxisRange = ScrollAxisRange(value = { 0f }, maxValue = { 0f })
            },
        state = lazyGridState,
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        items(model.lastYear() - model.startYear()) {
            val localYear = it + model.startYear()
            YearCell(
                localYear = localYear,
                isCurrent = localYear == year,
                onClick = {
                    scope.launch { onYearSelected(localYear) }
                    onDismiss()
                },
            )
        }
    }
}

/** 单个年份格子，当前年份高亮。 */
@Composable
private fun YearCell(
    localYear: Int,
    isCurrent: Boolean,
    onClick: () -> Unit,
) {
    Text(
        text = localYear.toString(),
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .background(
                if (isCurrent) MaterialTheme.colorScheme.surfaceContainerHigh else Color.Transparent,
                RoundedCornerShape(50)
            ),
        color = if (isCurrent) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onPrimary
        },
        fontSize = 18.sp,
        textAlign = TextAlign.Center
    )
}
