package com.example.fragment.project.components.calendar

import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import com.example.fragment.project.R
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun MonthPager(
    state: CalendarState,
    mode: CalendarMode,
    model: CalendarModel,
    monthModePagerState: PagerState,
    weekModePagerState: PagerState,
    onCalendarStateChange: (mode: CalendarMode) -> Unit,
    onSelectedDateChange: (year: Int, month: Int, day: Int) -> Unit,
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val isWeekMode = mode == CalendarMode.Week
    var selectedWeek by remember { mutableIntStateOf(0) }
    var selectedDate by remember { mutableStateOf(model.localCalendarDate()) }
    LaunchedEffect(state) {
        state.handleCalendarEvent(
            addSchedule = {
                scope.launch {
                    selectedDate?.addSchedule(it)
                }
            }
        )
    }
    val pagerState = if (isWeekMode) weekModePagerState else monthModePagerState
    //周模式和月模式联动
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collectLatest { page ->
            if (isWeekMode) {
                val month = model.weekModeByIndex(page) ?: return@collectLatest
                selectedWeek = model.weekByWeekModeIndex(page)
                val isDay = month.weeks[selectedWeek].firstOrNull { it.selectedDay.value }
                if (isDay == null) {
                    selectedDate?.selectedDay?.emit(false)
                    selectedDate = month.weeks[selectedWeek].firstOrNull { it.currMonth }
                    selectedDate?.selectedDay?.emit(true)
                }
                val index = (month.year - model.startYear()) * 12 + month.month - 1
                if (monthModePagerState.currentPage != index) {
                    scope.launch {
                        monthModePagerState.scrollToPage(index)
                    }
                }
            } else {
                val month = model.monthModeByIndex(page) ?: return@collectLatest
                val isDay = month.weeks[selectedWeek].firstOrNull { it.selectedDay.value }
                if (isDay == null) {
                    selectedWeek = 0
                    selectedDate?.selectedDay?.emit(false)
                    selectedDate = month.weeks[selectedWeek].firstOrNull { it.currMonth }
                    selectedDate?.selectedDay?.emit(true)
                }
                val index = model.weekModeIndexByDate(month.year, month.month, selectedWeek)
                if (weekModePagerState.currentPage != index) {
                    scope.launch {
                        weekModePagerState.scrollToPage(index)
                    }
                }
            }
        }
    }
    BoxWithConstraints {
        val height = maxHeight
        HorizontalPager(state = pagerState) { page ->
            val month = if (isWeekMode) {
                model.weekModeByIndex(page)
            } else {
                model.monthModeByIndex(page)
            } ?: return@HorizontalPager
            val weekModeHeight = with(density) { WeekHeight.toPx() }
            val monthModeHeight = with(density) { WeekHeight.toPx() * month.weeksInMonth() }
            val monthFillModeHeight = with(density) { (height - TipArrowHeight).toPx() }
            val anchoredDraggableState = remember(monthModeHeight, monthFillModeHeight) {
                AnchoredDraggableState(
                    initialValue = mode,
                    anchors = DraggableAnchors {
                        CalendarMode.Week at weekModeHeight
                        CalendarMode.Month at monthModeHeight
                        CalendarMode.MonthFill at monthFillModeHeight
                    },
                    positionalThreshold = { distance -> distance * 0.5f },
                    velocityThreshold = { with(density) { 100.dp.toPx() } },
                    snapAnimationSpec = TweenSpec(durationMillis = 350),
                    decayAnimationSpec = exponentialDecay(10f),
                )
            }
            //展开日历
            LaunchedEffect(model) {
                if (model.firstStartup) {
                    anchoredDraggableState.animateTo(CalendarMode.Month)
                    model.firstStartup = false
                }
            }
            //日历和日程联动
            var enabled by remember { mutableStateOf(true) }
            val listState = rememberLazyListState()
            LaunchedEffect(listState) {
                snapshotFlow { listState.isScrollInProgress }.collectLatest {
                    if (listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0 && !listState.canScrollForward) {
                        enabled = false
                    }
                }
            }
            LaunchedEffect(anchoredDraggableState) {
                snapshotFlow { anchoredDraggableState.currentValue }.collectLatest {
                    if (it == CalendarMode.Week) {
                        enabled = true
                    } else {
                        listState.animateScrollToItem(0)
                        enabled = false
                    }
                    onCalendarStateChange(it)
                }
            }
            Box(
                modifier = Modifier
                    .anchoredDraggable(
                        state = anchoredDraggableState,
                        orientation = Orientation.Vertical,
                    )
                    .background(colorResource(id = R.color.background))
                    .fillMaxSize()
                    .clipToBounds(),
            ) {
                val isMonthFillMode = anchoredDraggableState.currentValue == CalendarMode.MonthFill
                val anchoredDraggableOffset = anchoredDraggableState.requireOffset()
                WeekContent(
                    items = month.weeks,
                    selectedWeek = selectedWeek,
                    weekModeHeight = weekModeHeight,
                    monthModeHeight = monthModeHeight,
                    monthFillModeHeight = monthFillModeHeight,
                    isMonthFillMode = isMonthFillMode,
                    offsetProvider = { anchoredDraggableOffset },
                ) { date ->
                    DayContent(date, isMonthFillMode) {
                        scope.launch {
                            selectedDate?.selectedDay?.emit(false)
                            selectedDate = date
                            selectedDate?.selectedDay?.emit(true)
                        }
                        onSelectedDateChange(date.year, date.month, date.day)
                        selectedWeek = date.week
                        if (mode == CalendarMode.Week) {
                            return@DayContent
                        }
                        val index = model.weekModeIndexByDate(date.year, date.month, date.week)
                        if (weekModePagerState.currentPage != index) {
                            scope.launch {
                                weekModePagerState.scrollToPage(index)
                            }
                        }
                    }
                }
                ScheduleContent(
                    date = selectedDate,
                    mode = mode,
                    height = height,
                    listState = listState,
                    userScrollEnabled = enabled,
                    offsetProvider = { anchoredDraggableOffset.toInt() },
                )
            }
        }
    }
}