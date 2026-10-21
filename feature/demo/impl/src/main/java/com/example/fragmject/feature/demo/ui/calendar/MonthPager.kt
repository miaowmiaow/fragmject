package com.example.fragmject.feature.demo.ui.calendar

import android.annotation.SuppressLint
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
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
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.flow.collectLatest
import java.time.LocalDate

/** 月/周两个 Pager 的状态，成对出现才有意义，故合并为一个参数。 */
@Stable
internal class CalendarPagerStates(
    val monthModePagerState: PagerState,
    val weekModePagerState: PagerState,
)

/** 日期选择相关的三个回调；合并后避免 MonthPager 参数表继续膨胀。 */
@Stable
internal class CalendarDateCallbacks(
    val onSelectDate: (LocalDate) -> Unit,
    val onRemoveSchedule: (LocalDate, String) -> Unit,
    val onSelectedDateChange: (year: Int, month: Int, day: Int) -> Unit,
)

/** 单个 Pager 页共享的输入数据，避免页函数参数表随需求继续膨胀。 */
@Stable
private class MonthPagerPageInputs(
    val model: CalendarModel,
    val mode: CalendarMode,
    val selectedWeek: Int,
    val selectedDate: LocalDate?,
    val scheduleMap: Map<LocalDate, List<String>>,
)

@SuppressLint("UnusedBoxWithConstraintsScope", "FrequentlyChangingValue")
@Composable
internal fun MonthPager(
    state: CalendarState,
    model: CalendarModel,
    pagerStates: CalendarPagerStates,
    scheduleMap: Map<LocalDate, List<String>>,
    selectedDate: LocalDate?,
    callbacks: CalendarDateCallbacks,
) {
    val scope = rememberCoroutineScope()
    var mode by remember(state.mode) { mutableStateOf(state.mode) }
    val isWeekMode = mode == CalendarMode.Week
    var selectedWeek by remember { mutableIntStateOf(0) }
    val pagerState = if (isWeekMode) pagerStates.weekModePagerState else pagerStates.monthModePagerState
    val synchronizer = remember(model, pagerStates, callbacks) {
        CalendarPagerSynchronizer(model, pagerStates, callbacks) { selectedWeek = it }
    }
    //周模式和月模式联动
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collectLatest { page ->
            synchronizer.sync(
                page = page,
                isWeekMode = isWeekMode,
                selectedDate = selectedDate,
                selectedWeek = selectedWeek,
                scope = scope,
            )
        }
    }
    BoxWithConstraints {
        val height = maxHeight
        val inputs = MonthPagerPageInputs(model, mode, selectedWeek, selectedDate, scheduleMap)
        HorizontalPager(state = pagerState) { page ->
            MonthPagerPage(
                page = page,
                inputs = inputs,
                height = height,
                onModeSettled = { settled ->
                    state.mode = settled
                    mode = settled
                },
                onDateClick = { date -> synchronizer.selectDate(date, mode, scope) },
                onRemoveSchedule = callbacks.onRemoveSchedule,
            )
        }
    }
}

/** 单个月份的日历页：手势折叠状态 + 周内容 + 日程列表。 */
@SuppressLint("FrequentlyChangingValue")
@Composable
private fun MonthPagerPage(
    page: Int,
    inputs: MonthPagerPageInputs,
    height: Dp,
    onModeSettled: (CalendarMode) -> Unit,
    onDateClick: (CalendarDate) -> Unit,
    onRemoveSchedule: (LocalDate, String) -> Unit,
) {
    val density = LocalDensity.current
    val month = if (inputs.mode == CalendarMode.Week) {
        inputs.model.weekModeByIndex(page)
    } else {
        inputs.model.monthModeByIndex(page)
    } ?: return
    val weekModeHeight = with(density) { WeekHeight.toPx() }
    val monthModeHeight = with(density) { WeekHeight.toPx() * month.weeksInMonth() }
    val monthFillModeHeight = with(density) { (height - TipArrowHeight).toPx() }
    val listState = rememberLazyListState()
    val anchoredDraggableState = remember(monthModeHeight, monthFillModeHeight) {
        AnchoredDraggableState(
            initialValue = inputs.mode,
            anchors = DraggableAnchors {
                CalendarMode.Week at weekModeHeight
                CalendarMode.Month at monthModeHeight
                CalendarMode.MonthFill at monthFillModeHeight
            },
        )
    }
    LaunchedEffect(anchoredDraggableState) {
        snapshotFlow { anchoredDraggableState.settledValue }.collectLatest {
            onModeSettled(anchoredDraggableState.currentValue)
        }
    }
    //展开日历
    LaunchedEffect(inputs.model) {
        if (inputs.model.firstStartup) {
            anchoredDraggableState.animateTo(CalendarMode.Month)
            inputs.model.firstStartup = false
        }
    }
    Box(
        modifier = Modifier
            .anchoredDraggable(
                state = anchoredDraggableState,
                orientation = Orientation.Vertical,
            )
            .background(MaterialTheme.colorScheme.background)
            .fillMaxSize()
            .clipToBounds(),
    ) {
        val isMonthFillMode = anchoredDraggableState.currentValue == CalendarMode.MonthFill
        val anchoredDraggableOffset = anchoredDraggableState.requireOffset()
        WeekContent(
            items = month.weeks,
            selectedWeek = inputs.selectedWeek,
            heights = WeekContentHeights(weekModeHeight, monthModeHeight, monthFillModeHeight),
            isMonthFillMode = isMonthFillMode,
            offsetProvider = { anchoredDraggableOffset },
        ) { date ->
            DayContent(
                date = date,
                isMonthFillMode = isMonthFillMode,
                schedule = inputs.scheduleMap[date.localDate].orEmpty(),
                isSelected = date.localDate == inputs.selectedDate,
            ) {
                onDateClick(date)
            }
        }
        ScheduleContent(
            spec = ScheduleContentSpec(
                date = inputs.selectedDate?.let { inputs.model.calendarDate(it) },
                mode = inputs.mode,
                height = height,
                schedule = inputs.selectedDate?.let { inputs.scheduleMap[it].orEmpty() }.orEmpty(),
            ),
            listState = listState,
            userScrollEnabled = { inputs.mode == CalendarMode.Week },
            offsetProvider = { anchoredDraggableOffset.toInt() },
            onRemoveSchedule = { text ->
                inputs.selectedDate?.let { onRemoveSchedule(it, text) }
            },
        )
    }
}
