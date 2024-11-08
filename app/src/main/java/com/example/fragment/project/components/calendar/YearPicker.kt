package com.example.fragment.project.components.calendar

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
    val year = if (isWeekMode) {
        model.yearByWeekModeIndex(weekModePagerState.currentPage)
    } else {
        model.startYear() + monthModePagerState.currentPage / 12
    }
    val month = if (isWeekMode) {
        model.monthByWeekModeIndex(weekModePagerState.currentPage)
    } else {
        monthModePagerState.currentPage % 12 + 1
    }
    Column {
        var visible by remember { mutableStateOf(false) }
        TextButton(
            onClick = { visible = !visible },
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
        AnimatedVisibility(
            visible = visible,
            modifier = Modifier.clipToBounds(),
            enter = expandVertically() + fadeIn(initialAlpha = 0.6f),
            exit = shrinkVertically() + fadeOut()
        ) {
            val scope = rememberCoroutineScope()
            val lazyGridState = rememberLazyGridState(
                initialFirstVisibleItemIndex = max(0, year - model.startYear() - YearInRow)
            )
            LazyVerticalGrid(
                columns = GridCells.Fixed(YearInRow),
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
                    Text(
                        text = localYear.toString(),
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable {
                                scope.launch {
                                    if (isWeekMode) {
                                        val page = model.weekModeIndexByDate(localYear, month)
                                        weekModePagerState.scrollToPage(page)
                                    } else {
                                        val page = it * 12 + month - 1
                                        monthModePagerState.scrollToPage(page)
                                    }
                                }
                                visible = false
                            }
                            .background(
                                if (localYear == year) MaterialTheme.colorScheme.surfaceContainerHigh else Color.Transparent,
                                RoundedCornerShape(50)
                            ),
                        color = if (localYear == year) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onPrimary,
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
