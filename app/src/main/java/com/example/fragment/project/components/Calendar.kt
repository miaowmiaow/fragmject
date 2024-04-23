package com.example.fragment.project.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.ScrollAxisRange
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.verticalScrollAxisRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.zIndex
import com.example.fragment.project.R
import com.example.fragment.project.utils.DateInfo
import com.example.fragment.project.utils.getLunarDate
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun Calendar(
    state: CalendarState = rememberCalendarState(),
    padding: Dp = 0.dp,
    hasCustomCalendar: (date: DateInfo) -> Boolean,
    contentPadding: PaddingValues = PaddingValues(10.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(10.dp),
    content: LazyListScope.(date: DateInfo) -> Unit
) {
    val density = LocalDensity.current
    val locale = LocalConfiguration.current.locales[0]
    val coroutineScope = rememberCoroutineScope()
    val weekdays = with(locale) {
        DayOfWeek.entries.map {
            it.getDisplayName(TextStyle.FULL, this) to it.getDisplayName(TextStyle.NARROW, this)
        }
    }
    val firstDayOfWeek = WeekFields.of(locale).firstDayOfWeek.value
    val dayNames = arrayListOf<Pair<String, String>>()
    for (i in firstDayOfWeek - 1 until weekdays.size) {
        dayNames.add(weekdays[i])
    }
    for (i in 0 until firstDayOfWeek - 1) {
        dayNames.add(weekdays[i])
    }
    val yearRange = state.yearRange
    val monthsRange = (yearRange.last - yearRange.first + 1) * 12
    val firstYear = yearRange.first
    //当月周数
    var weeksInMonth by remember { mutableIntStateOf(CalendarDefaults.MAX_WEEKS_IN_MONTH) }
    var selectedWeek by remember { mutableIntStateOf(0) }
    var calendarState by remember { mutableStateOf(CalendarDefaults.DragAnchors.MONTH) }
    val initialPage = (state.selectedYear.value - firstYear) * 12 + state.selectedMonth.value - 1
    val pagerState = rememberPagerState(initialPage) { monthsRange }
    //当月第一天是星期几
    var firstDayOfMonth by remember { mutableIntStateOf(0) }
    var currentPage = pagerState.currentPage
    val data = remember { mutableListOf<List<DateInfo>>() }
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect {
            var year: Int
            var month: Int
            //实现周和月数据的切换
            if (calendarState == CalendarDefaults.DragAnchors.WEEK) {
                year = state.selectedYear.value
                month = state.selectedMonth.value
                if (pagerState.currentPage - currentPage > 0) {
                    selectedWeek += 1
                    if (selectedWeek >= weeksInMonth) {
                        month = state.selectedMonth.value + 1
                        if (month > 12) {
                            year = state.selectedYear.value + 1
                            month = 1
                        }
                        selectedWeek = 0
                    }
                } else {
                    selectedWeek -= 1
                    if (selectedWeek < 0) {
                        month = state.selectedMonth.value - 1
                        if (month < 1) {
                            year = state.selectedYear.value - 1
                            month = 12
                        }
                        val date = LocalDate.of(year, month, 1)
                        val daysInMonth = date.lengthOfMonth()
                        val difference = date.dayOfWeek.value - firstDayOfWeek
                        firstDayOfMonth = if (difference < 0) {
                            difference + CalendarDefaults.DAYS_IN_WEEK
                        } else {
                            difference
                        }
                        val days = (daysInMonth + firstDayOfMonth).toDouble()
                        selectedWeek = floor(days / CalendarDefaults.DAYS_IN_WEEK).toInt()
                    }
                }
            } else {
                year = firstYear + pagerState.currentPage / 12
                month = pagerState.currentPage % 12 + 1
            }
            currentPage = pagerState.currentPage
            if (year != state.selectedYear.value || month != state.selectedMonth.value) {
                state.selectedDay.value = 1
                if (calendarState != CalendarDefaults.DragAnchors.WEEK) {
                    selectedWeek = 0
                }
            }
            state.selectedYear.value = year
            state.selectedMonth.value = month
            val date = LocalDate.of(year, month, 1)
            val daysInMonth = date.lengthOfMonth()
            val difference = date.dayOfWeek.value - firstDayOfWeek
            firstDayOfMonth = if (difference < 0) {
                difference + CalendarDefaults.DAYS_IN_WEEK
            } else {
                difference
            }
            val days = (daysInMonth + firstDayOfMonth).toDouble()
            weeksInMonth = ceil(days / CalendarDefaults.DAYS_IN_WEEK).toInt()
            data.clear()
            var cellIndex = 0
            for (weekIndex in 0 until weeksInMonth) {
                val dates = mutableListOf<DateInfo>()
                val lastDate = date.plusMonths(-1)
                val daysInLastMonth = lastDate.lengthOfMonth()
                for (dayIndex in 0 until CalendarDefaults.DAYS_IN_WEEK) {
                    var y = state.selectedYear.value
                    var m = state.selectedMonth.value
                    if (cellIndex < firstDayOfMonth) {
                        val d = daysInLastMonth - (firstDayOfMonth - cellIndex) + 1
                        m -= 1
                        if (m < 1) {
                            y -= 1
                            m = 12
                        }
                        dates.add(getLunarDate(y, m, d))
                    } else if (cellIndex >= (firstDayOfMonth + daysInMonth)) {
                        val d = cellIndex - (firstDayOfMonth + daysInMonth) + 1
                        m += 1
                        if (m > 12) {
                            y += 1
                            m = 1
                        }
                        dates.add(getLunarDate(y, m, d))
                    } else {
                        val d = cellIndex - firstDayOfMonth + 1
                        if (calendarState != CalendarDefaults.DragAnchors.WEEK && m == state.selectedMonth.value && d == state.selectedDay.value) {
                            selectedWeek = weekIndex
                        }
                        dates.add(getLunarDate(y, m, d))
                    }
                    cellIndex++
                }
                data.add(dates)
            }
        }
    }
    var scrollEnabled by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }
            .collect {
                if (listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0) {
                    scrollEnabled = false
                }
            }
    }
    val monthHeight = CalendarDefaults.WeekHeight * weeksInMonth
    val screenHeightDp = LocalConfiguration.current.screenHeightDp.dp
    val monthExpandHeight =
        screenHeightDp - padding - CalendarDefaults.YearHeight - CalendarDefaults.DayHeight
    val draggableState = remember(weeksInMonth) {
        AnchoredDraggableState(
            initialValue = calendarState,
            animationSpec = TweenSpec(durationMillis = 350),
            anchors = DraggableAnchors {
                CalendarDefaults.DragAnchors.WEEK at with(density) { CalendarDefaults.WeekHeight.toPx() }
                CalendarDefaults.DragAnchors.MONTH at with(density) { monthHeight.toPx() }
                CalendarDefaults.DragAnchors.MONTH_EXPAND at with(density) { monthExpandHeight.toPx() }
            },
            positionalThreshold = { distance -> distance * 0.5f },
            velocityThreshold = { with(density) { 100.dp.toPx() } },
        )
    }
    LaunchedEffect(draggableState) {
        snapshotFlow { draggableState.currentValue }
            .collectLatest {
                calendarState = it
                scrollEnabled =
                    it == CalendarDefaults.DragAnchors.WEEK && listState.canScrollForward
                val monthPage =
                    (state.selectedYear.value - firstYear) * 12 + state.selectedMonth.value - 1
                //切换到正确的月份
                if (it != CalendarDefaults.DragAnchors.WEEK && monthPage != pagerState.currentPage) {
                    pagerState.scrollToPage(monthPage)
                }
            }
    }

    Column {
        var yearPickerVisible by remember { mutableStateOf(false) }
        TextButton(
            onClick = {
                yearPickerVisible = !yearPickerVisible
            },
            modifier = Modifier.height(CalendarDefaults.YearHeight),
            shape = CircleShape,
            colors = ButtonDefaults.textButtonColors(contentColor = LocalContentColor.current),
            elevation = null,
            border = null,
        ) {
            Text(
                text = "${state.selectedYear.value}年${state.selectedMonth.value}月",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
            Icon(
                Icons.Filled.ArrowDropDown,
                contentDescription = "",
                Modifier.rotate(if (yearPickerVisible) 180f else 0f)
            )
        }
        AnimatedVisibility(
            visible = yearPickerVisible,
            modifier = Modifier.clipToBounds(),
            enter = expandVertically() + fadeIn(initialAlpha = 0.6f),
            exit = shrinkVertically() + fadeOut()
        ) {
            YearPicker(state.selectedYear.value, yearRange) {
                coroutineScope.launch {
                    pagerState.scrollToPage(pagerState.currentPage + (it - state.selectedYear.value) * 12)
                }
                yearPickerVisible = false
            }
        }
        WeekDays(dayNames)
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .anchoredDraggable(
                    state = draggableState,
                    orientation = Orientation.Vertical,
                )
        ) {
            Box {
                val monthOffset = with(density) { draggableState.offset.toDp() }
                val scheduleHeight =
                    screenHeightDp - padding - CalendarDefaults.YearHeight - CalendarDefaults.DayHeight - monthOffset
                val monthExpand =
                    draggableState.currentValue == CalendarDefaults.DragAnchors.MONTH_EXPAND
                Month(
                    data,
                    monthExpandHeight,
                    monthOffset,
                    monthExpand,
                    weeksInMonth,
                    selectedWeek,
                ) { weekIndex, date ->
                    Day(
                        date,
                        monthExpand,
                        state.selectedMonth.value,
                        state.selectedDay.value,
                    ) {
                        selectedWeek = weekIndex
                        state.selectedYear.value = date.year
                        state.selectedMonth.value = date.month
                        state.selectedDay.value = date.day
                    }
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(scheduleHeight)
                        .offset(0.dp, monthOffset)
                        .background(colorResource(id = R.color.background))
                ) {
                    val selectedDate = state.getSelectedDate()
                    Text(
                        text = selectedDate.lunarYear + " " + selectedDate.animalsYear + " " + selectedDate.lunarMonth + selectedDate.lunarDay,
                        modifier = Modifier.padding(10.dp)
                    )
                    if (!hasCustomCalendar(selectedDate) && selectedDate.getFestival().isEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Image(
                                painter = painterResource(R.mipmap.ic_calendar),
                                contentDescription = null,
                            )
                            Spacer(Modifier.size(10.dp))
                            Text(
                                text = "没有日程",
                                fontSize = 14.sp,
                                color = colorResource(R.color.text_999)
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            state = listState,
                            contentPadding = contentPadding,
                            verticalArrangement = verticalArrangement,
                            userScrollEnabled = scrollEnabled
                        ) {
                            content(selectedDate)
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun YearPicker(
    currentYear: Int,
    yearRange: IntRange,
    onYearSelected: (year: Int) -> Unit
) {
    val yearsInRow = 3
    val lazyGridState = rememberLazyGridState(
        initialFirstVisibleItemIndex = max(
            0, currentYear - yearRange.first - yearsInRow
        )
    )
    LazyVerticalGrid(
        columns = GridCells.Fixed(yearsInRow),
        modifier = Modifier
            .background(colorResource(R.color.background))
            .semantics {
                verticalScrollAxisRange = ScrollAxisRange(value = { 0f }, maxValue = { 0f })
            },
        state = lazyGridState,
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        items(yearRange.count()) {
            val localizedYear = it + yearRange.first
            Text(
                text = localizedYear.toString(),
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable { onYearSelected(localizedYear) }
                    .background(
                        colorResource(if (localizedYear == currentYear) R.color.theme else R.color.transparent),
                        RoundedCornerShape(50)
                    ),
                color = colorResource(if (localizedYear == currentYear) R.color.text_fff else R.color.text_333),
                fontSize = 18.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
internal fun WeekDays(
    dayNames: ArrayList<Pair<String, String>>
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        dayNames.fastForEach {
            Text(
                text = it.second,
                modifier = Modifier
                    .weight(1f)
                    .height(CalendarDefaults.DayHeight),
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
internal fun Month(
    data: List<List<DateInfo>>,
    monthExpandHeight: Dp,
    monthOffset: Dp,
    monthExpand: Boolean,
    weeksInMonth: Int,
    selectedWeek: Int,
    content: @Composable BoxScope.(weekIndex: Int, date: DateInfo) -> Unit
) {
    val monthHeight = CalendarDefaults.WeekHeight * weeksInMonth
    val animatedColor by animateColorAsState(
        if (monthExpand) colorResource(R.color.white) else colorResource(R.color.background),
        animationSpec = TweenSpec(350),
        label = "color"
    )

    Box(
        modifier = Modifier
            .background(colorResource(id = R.color.background))
            .fillMaxWidth()
            .height(monthExpandHeight)
            .clipToBounds(),
    ) {
        for (weekIndex in 0 until weeksInMonth) {
            var zIndex = 0f
            val currWeekOffset = CalendarDefaults.WeekHeight * weekIndex
            var weekHeight = monthExpandHeight / weeksInMonth
            val offsetY = if (monthOffset <= monthHeight) {
                weekHeight = CalendarDefaults.WeekHeight
                val offset = currWeekOffset + monthOffset - monthHeight
                if (selectedWeek == weekIndex && offset < 0.dp) {
                    zIndex = 1f
                    0.dp
                } else {
                    zIndex = 0f
                    offset
                }
            } else {
                monthOffset / weeksInMonth * weekIndex
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(weekHeight)
                    .offset(0.dp, offsetY)
                    .zIndex(zIndex)
                    .then(if (monthExpand) Modifier.padding(bottom = 1.dp) else Modifier)
                    .background(animatedColor),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.Top
            ) {
                for (dayIndex in 0 until CalendarDefaults.DAYS_IN_WEEK) {
                    if (data.size == weeksInMonth) {
                        Box(modifier = Modifier.weight(1f)) {
                            content(weekIndex, data[weekIndex][dayIndex])
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun Day(
    date: DateInfo,
    monthExpand: Boolean,
    currMonth: Int,
    currDay: Int,
    onClick: () -> Unit = {}
) {
    Column {
        Column(modifier = Modifier
            .fillMaxWidth()
            .padding(1.dp)
            .clip(CircleShape)
            .clickable { onClick() }
            .then(
                if (date.month == currMonth && date.day == currDay)
                    Modifier.border(1.dp, colorResource(R.color.theme_orange), CircleShape)
                else
                    Modifier
            )
        ) {
            Text(
                text = date.day.toString(),
                modifier = Modifier
                    .fillMaxWidth()
                    .clipToBounds(),
                color = colorResource(if (date.month == currMonth) R.color.text_333 else R.color.text_999),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = "  ${if (monthExpand) date.lunarDay else date.getFirstFestival()}  ",
                modifier = Modifier
                    .fillMaxWidth()
                    .clipToBounds(),
                color = colorResource(
                    if (date.isFestival() && !monthExpand) {
                        if (date.month == currMonth) {
                            R.color.theme_orange
                        } else {
                            R.color.b_zero_theme_orange
                        }
                    } else {
                        R.color.text_999
                    }
                ),
                fontSize = 10.sp,
                textAlign = TextAlign.Center,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
            )
        }
        if (monthExpand) {
            date.getFestival().forEach {
                Text(
                    text = it,
                    modifier = Modifier
                        .padding(horizontal = 3.dp, vertical = 1.dp)
                        .background(
                            colorResource(R.color.gray_e5),
                            RoundedCornerShape(3.dp)
                        )
                        .fillMaxWidth(),
                    color = colorResource(R.color.text_999),
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 14.sp,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                )
            }
        }
    }
}

object CalendarDefaults {

    const val DAYS_IN_WEEK = 7
    const val MAX_WEEKS_IN_MONTH = 6
    val YearRange: IntRange = IntRange(1900, 2100)
    val YearHeight = 45.dp
    val WeekHeight = 70.dp
    val DayHeight = 45.dp

    enum class DragAnchors {
        WEEK,
        MONTH,
        MONTH_EXPAND,
    }
}

@Stable
class CalendarState(
    var yearRange: IntRange,
    var selectedYear: MutableState<Int>,
    var selectedMonth: MutableState<Int>,
    var selectedDay: MutableState<Int>,
) {
    fun getSelectedDate(): DateInfo {
        return getLunarDate(
            selectedYear.value,
            selectedMonth.value,
            selectedDay.value
        )
    }
}

@Composable
@ExperimentalMaterial3Api
fun rememberCalendarState(
    yearRange: IntRange = CalendarDefaults.YearRange,
    selectedYear: MutableState<Int>? = null,
    selectedMonth: MutableState<Int>? = null,
    selectedDay: MutableState<Int>? = null,
): CalendarState {
    val localDate = LocalDate.now()
    val state = remember(localDate) {
        val year = selectedYear ?: mutableIntStateOf(localDate.year)
        val month = selectedMonth ?: mutableIntStateOf(localDate.monthValue)
        val day = selectedDay ?: mutableIntStateOf(localDate.dayOfMonth)
        CalendarState(yearRange, year, month, day)
    }
    return state
}