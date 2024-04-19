package com.example.fragment.project.components

import android.os.Build
import androidx.annotation.RequiresApi
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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import kotlin.math.max

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun Calendar(
    state: CalendarState = rememberCalendarState(),
    padding: Dp = 0.dp,
    hasCustomCalendar: (date: DateInfo) -> Boolean,
    contentPadding: PaddingValues = PaddingValues(10.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(10.dp),
    content: LazyListScope.(date: DateInfo) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val locale = LocalConfiguration.current.locales[0]
    val weekdays: List<Pair<String, String>> = with(locale) {
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
    var selectedMonthRows by remember { mutableIntStateOf(CalendarDefaults.MAX_MONTH_ROWS) }
    var selectedWeek by remember { mutableIntStateOf(0) }
    val initialPage = (state.selectedYear.value - firstYear) * 12 + state.selectedMonth.value - 1
    val pagerState = rememberPagerState(initialPage) { monthsRange }
    var firstDayOfMonth by remember { mutableIntStateOf(0) }
    var yearPickerVisible by remember { mutableStateOf(false) }
    var monthExpand by remember { mutableStateOf(false) }
    val data = remember { mutableListOf<List<DateInfo>>() }
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect {
            val year = firstYear + pagerState.currentPage / 12
            val month = pagerState.currentPage % 12 + 1
            if (year != state.selectedYear.value || month != state.selectedMonth.value) {
                state.selectedDay.value = 1
                selectedWeek = 0
            }
            state.selectedYear.value = year
            state.selectedMonth.value = month
            val date = LocalDate.of(year, month, 1)
            val difference = date.dayOfWeek.value - firstDayOfWeek
            //每月第一天星期几
            firstDayOfMonth = if (difference < 0) {
                difference + CalendarDefaults.DAYS_IN_WEEK
            } else {
                difference
            }
            val daysInMonth = date.lengthOfMonth()
            val days = (daysInMonth + firstDayOfMonth).toDouble()
            selectedMonthRows = ceil(days / CalendarDefaults.DAYS_IN_WEEK).toInt()
            data.clear()
            var cellIndex = 0
            for (weekIndex in 0 until selectedMonthRows) {
                val dates = mutableListOf<DateInfo>()
                val lastDate = date.plusMonths(-1)
                val daysInLastMonth = lastDate.lengthOfMonth()
                for (dayIndex in 0 until CalendarDefaults.DAYS_IN_WEEK) {
                    var y = state.selectedYear.value
                    var m = state.selectedMonth.value
                    if (cellIndex < firstDayOfMonth) {
                        val d = daysInLastMonth - (firstDayOfMonth - cellIndex) + 1
                        m -= 1
                        if (m - 1 < 1) {
                            y -= 1
                            m = m - 1 + 12
                        }
                        dates.add(getLunarDate(y, m, d))
                    } else if (cellIndex >= (firstDayOfMonth + daysInMonth)) {
                        m += 1
                        val d = cellIndex - (firstDayOfMonth + daysInMonth) + 1
                        if (m + 1 > 12) {
                            y += 1
                            m = m + 1 - 12
                        }
                        dates.add(getLunarDate(y, m, d))
                    } else {
                        val d = cellIndex - firstDayOfMonth + 1
                        if (m == state.selectedMonth.value && d == state.selectedDay.value) {
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
    val anchoredDraggableState = remember(selectedMonthRows) {
        AnchoredDraggableState(
            initialValue = CalendarDefaults.DragAnchors.Center,
            animationSpec = TweenSpec(durationMillis = 350),
            anchors = DraggableAnchors {
                CalendarDefaults.DragAnchors.Start at with(density) { CalendarDefaults.WeekHeight.toPx() }
                CalendarDefaults.DragAnchors.Center at with(density) { (CalendarDefaults.WeekHeight * selectedMonthRows).toPx() }
                CalendarDefaults.DragAnchors.End at with(density) { (configuration.screenHeightDp.dp - padding - CalendarDefaults.YearHeight - CalendarDefaults.WeekHeight).toPx() }
            },
            positionalThreshold = { distance -> distance * 0.5f },
            velocityThreshold = { with(density) { 100.dp.toPx() } },
        )
    }
    LaunchedEffect(anchoredDraggableState) {
        snapshotFlow { anchoredDraggableState.currentValue }
            .collectLatest {
                monthExpand = it == CalendarDefaults.DragAnchors.End
                scrollEnabled = (it == CalendarDefaults.DragAnchors.Start)
                        && listState.canScrollForward
            }
    }
    val calendarHeight = CalendarDefaults.WeekHeight * selectedMonthRows
    val monthHeight = with(density) { anchoredDraggableState.offset.toDp() }

    Column {
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
                    state = anchoredDraggableState,
                    orientation = Orientation.Vertical,
                )
        ) {
            Column {
                Month(
                    data,
                    calendarHeight,
                    monthHeight,
                    selectedMonthRows,
                    selectedWeek,
                    monthExpand
                ) { weekIndex, date ->
                    Day(
                        date,
                        state.selectedMonth.value,
                        state.selectedDay.value,
                        monthExpand
                    ) {
                        selectedWeek = weekIndex
                        state.selectedYear.value = date.year
                        state.selectedMonth.value = date.month
                        state.selectedDay.value = date.day
                    }
                }
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
        modifier = Modifier
            .fillMaxWidth()
            .height(CalendarDefaults.WeekHeight),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        dayNames.fastForEach {
            Text(
                text = it.second,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
internal fun Month(
    data: List<List<DateInfo>>,
    calendarHeight: Dp,
    monthHeight: Dp,
    selectedMonthRows: Int,
    selectedWeek: Int,
    monthExpand: Boolean,
    content: @Composable RowScope.(weekIndex: Int, date: DateInfo) -> Unit
) {
    val animatedColor by animateColorAsState(
        if (monthExpand) colorResource(R.color.white) else colorResource(R.color.background),
        animationSpec = TweenSpec(350),
        label = "color"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(monthHeight)
            .clipToBounds(),
    ) {
        for (weekIndex in 0 until selectedMonthRows) {
            val isWeek = selectedWeek == weekIndex
            val currWeekOffset = CalendarDefaults.WeekHeight * weekIndex
            var weekHeight = monthHeight / selectedMonthRows
            val offsetY = if (monthHeight < calendarHeight) {
                weekHeight = CalendarDefaults.WeekHeight
                val offset = currWeekOffset + monthHeight - calendarHeight
                if (isWeek && offset < 0.dp) 0.dp else offset
            } else {
                monthHeight / selectedMonthRows * weekIndex
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(weekHeight)
                    .offset(0.dp, offsetY)
                    .zIndex(if (isWeek) 1f else 0f)
                    .then(if (monthExpand) Modifier.padding(bottom = 1.dp) else Modifier)
                    .background(animatedColor),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.Top
            ) {
                for (dayIndex in 0 until CalendarDefaults.DAYS_IN_WEEK) {
                    if (data.size == selectedMonthRows) {
                        val date = data[weekIndex][dayIndex]
                        content(weekIndex, date)
                    }
                }
            }
        }
    }
}

@Composable
internal fun Day(
    date: DateInfo,
    currMonth: Int,
    currDay: Int,
    expand: Boolean,
    onClick: () -> Unit = {}
) {
    Column {
        Column(modifier = Modifier
            .size(CalendarDefaults.WeekHeight)
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
                    .width(CalendarDefaults.WeekHeight)
                    .clipToBounds(),
                color = colorResource(if (date.month == currMonth) R.color.text_333 else R.color.text_999),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = if (expand) date.lunarDay else date.getFirstFestival(),
                modifier = Modifier
                    .width(CalendarDefaults.WeekHeight)
                    .clipToBounds(),
                color = colorResource(
                    if (date.isFestival() && !expand) {
                        if (date.month == currMonth) {
                            R.color.theme_orange
                        } else {
                            R.color.b_zero_theme_orange
                        }
                    } else {
                        R.color.text_999
                    }
                ),
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
            )
        }
        date.getFestival().forEach {
            Text(
                text = it,
                modifier = Modifier
                    .padding(vertical = 1.dp)
                    .background(
                        colorResource(R.color.gray_e5),
                        RoundedCornerShape(3.dp)
                    )
                    .width(CalendarDefaults.WeekHeight)
                    .height(20.dp),
                color = colorResource(R.color.text_999),
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
            )
        }
    }
}

object CalendarDefaults {

    const val DAYS_IN_WEEK = 7
    const val MAX_MONTH_ROWS = 6
    val YearRange: IntRange = IntRange(1900, 2100)
    val YearHeight = 50.dp
    val WeekHeight = 50.dp

    enum class DragAnchors {
        Start,
        Center,
        End,
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

@RequiresApi(Build.VERSION_CODES.O)
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