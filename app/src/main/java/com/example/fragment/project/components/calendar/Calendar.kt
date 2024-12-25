package com.example.fragment.project.components.calendar

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Calendar(
    state: CalendarState,
    modifier: Modifier = Modifier,
    onSelectedDateChange: (year: Int, month: Int, day: Int) -> Unit,
) {

    val defaultLocale = LocalConfiguration.current.locales[0]
    val model = remember(defaultLocale) { CalendarModel(defaultLocale) }

    val weekModePagerState = rememberCalendarPagerState(
        initialPage = model.weekModeIndexByLocalDate()
    ) { model.weekModeCount() }

    val monthModePagerState = rememberCalendarPagerState(
        initialPage = (model.localYear - model.startYear()) * 12 + model.localMonth - 1
    ) { model.monthModeCount() }

    Column(modifier = modifier) {
        YearPicker(
            mode = state.mode,
            model = model,
            weekModePagerState = weekModePagerState,
            monthModePagerState = monthModePagerState,
        )
        LazyVerticalGrid(
            columns = GridCells.Fixed(DAYS_IN_WEEK),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(model.dayNames) {
                Text(
                    text = it.second,
                    modifier = Modifier
                        .weight(1f)
                        .height(WeekDayHeight),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
        MonthPager(
            state = state,
            model = model,
            monthModePagerState = monthModePagerState,
            weekModePagerState = weekModePagerState,
            onSelectedDateChange = onSelectedDateChange,
        )
    }
}

@Stable
class CalendarState(
    private val scope: CoroutineScope
) {
    var mode = CalendarMode.Week

    private sealed interface CalendarEvent {
        data class Schedule(val text: String) : CalendarEvent
    }

    private val calendarEvents: MutableSharedFlow<CalendarEvent> = MutableSharedFlow()

    internal suspend fun handleCalendarEvent(
        addSchedule: (String) -> Unit = {},
    ): Nothing = withContext(Dispatchers.Main) {
        calendarEvents.collect { event ->
            if (event is CalendarEvent.Schedule) {
                addSchedule(event.text)
            }
        }
    }

    fun addSchedule(text: String) {
        scope.launch { calendarEvents.emit(CalendarEvent.Schedule(text)) }
    }

}

@Composable
fun rememberCalendarState(
    scope: CoroutineScope = rememberCoroutineScope()
): CalendarState = remember(scope) { CalendarState(scope) }

internal const val YearInRow = 3
internal val YearHeight = 45.dp
internal val WeekDayHeight = 45.dp
internal val WeekHeight = 70.dp
internal val LunarHeight = 40.dp
internal val TipArrowHeight = 25.dp

@Immutable
@JvmInline
value class CalendarMode internal constructor(internal val value: Int) {

    companion object {
        val Week = CalendarMode(0)
        val Month = CalendarMode(1)
        val MonthFill = CalendarMode(2)
    }

    override fun toString() = when (this) {
        Week -> "Week"
        Month -> "Month"
        MonthFill -> "MonthFill"
        else -> "Unknown"
    }
}

@ExperimentalFoundationApi
@Composable
fun rememberCalendarPagerState(
    initialPage: Int = 0,
    initialPageOffsetFraction: Float = 0f,
    pageCount: () -> Int
): PagerState {
    return remember {
        CalendarPagerState(initialPage, initialPageOffsetFraction, pageCount)
    }.apply {
        pageCountState.value = pageCount
    }
}

@ExperimentalFoundationApi
internal class CalendarPagerState(
    initialPage: Int,
    initialPageOffsetFraction: Float,
    updatedPageCount: () -> Int
) : PagerState(initialPage, initialPageOffsetFraction) {
    var pageCountState = mutableStateOf(updatedPageCount)
    override val pageCount: Int get() = pageCountState.value.invoke()
}