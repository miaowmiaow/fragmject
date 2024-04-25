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
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Calendar(
    padding: Dp = 0.dp,
    hasCustomCalendar: (date: DateInfo) -> Boolean,
    contentPadding: PaddingValues = PaddingValues(10.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(10.dp),
    content: LazyListScope.(date: DateInfo) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val locale = LocalConfiguration.current.locales[0]
    val firstDayOfWeek = WeekFields.of(locale).firstDayOfWeek.value
    val weekdays = with(locale) {
        DayOfWeek.entries.map {
            it.getDisplayName(TextStyle.FULL, this) to it.getDisplayName(TextStyle.NARROW, this)
        }
    }
    val dayNames = arrayListOf<Pair<String, String>>()
    for (i in firstDayOfWeek - 1 until weekdays.size) {
        dayNames.add(weekdays[i])
    }
    for (i in 0 until firstDayOfWeek - 1) {
        dayNames.add(weekdays[i])
    }

    var calendarState by remember { mutableStateOf(CalendarDefaults.DragAnchors.MONTH) }

    val yearRange = IntRange(1900, 2100)

    var localDate = LocalDate.now()
    val currYear = localDate.year
    val currMonth = localDate.monthValue
    val currDay = localDate.dayOfMonth

    val date = remember {
        mutableListOf<MonthInfo>().also {
            for (year in yearRange.first until yearRange.last) {
                for (month in 1..12) {
                    localDate = localDate.withYear(year).withMonth(month).withDayOfMonth(1)
                    val daysInMonth = localDate.lengthOfMonth()
                    val difference = localDate.dayOfWeek.value - firstDayOfWeek
                    val firstDayOfMonth = if (difference < 0) {
                        difference + CalendarDefaults.DAYS_IN_WEEK
                    } else {
                        difference
                    }
                    val days = (daysInMonth + firstDayOfMonth).toDouble()
                    val weeksInMonth = ceil(days / CalendarDefaults.DAYS_IN_WEEK).toInt()
                    var cellIndex = 0
                    var selectedWeek = 0
                    val data = mutableListOf<List<DayInfo>>()
                    for (weekIndex in 0 until weeksInMonth) {
                        val weekDate = mutableListOf<DayInfo>()
                        for (dayIndex in 0 until CalendarDefaults.DAYS_IN_WEEK) {
                            var y = year
                            var m = month
                            if (cellIndex < firstDayOfMonth) {
                                val lastMonth = localDate.plusMonths(-1)
                                val daysInLastMonth = lastMonth.lengthOfMonth()
                                val d = daysInLastMonth - (firstDayOfMonth - cellIndex) + 1
                                m -= 1
                                if (m < 1) {
                                    y -= 1
                                    m = 12
                                }
                                weekDate.add(DayInfo(y, m, d))
                            } else if (cellIndex >= (firstDayOfMonth + daysInMonth)) {
                                val d = cellIndex - (firstDayOfMonth + daysInMonth) + 1
                                m += 1
                                if (m > 12) {
                                    y += 1
                                    m = 1
                                }
                                weekDate.add(DayInfo(y, m, d))
                            } else {
                                val d = cellIndex - firstDayOfMonth + 1
                                if (y == currYear && m == currMonth && d == currDay) {
                                    selectedWeek = weekIndex
                                }
                                weekDate.add(DayInfo(y, m, d))
                            }
                            cellIndex++
                        }
                        data.add(weekDate)
                    }
                    it.add(
                        MonthInfo(year, month, data, firstDayOfMonth, weeksInMonth, selectedWeek)
                    )
                }
            }
        }
    }

    val firstYear = yearRange.first
    val pageCount = (yearRange.last - yearRange.first + 1) * 12
    val initialPage = (currYear - firstYear) * 12 + currMonth - 1
    val pagerState = rememberPagerState(initialPage) { pageCount }

    val density = LocalDensity.current
    val screenHeightDp = LocalConfiguration.current.screenHeightDp.dp
    val monthExpandHeightDp =
        screenHeightDp - padding - CalendarDefaults.YearHeight - CalendarDefaults.DayHeight
    var selectedYear by remember { mutableIntStateOf(currYear) }
    var selectedMonth by remember { mutableIntStateOf(currMonth) }
    var selectedDay by remember { mutableIntStateOf(currDay) }

    Column {
        var yearPickerVisible by remember { mutableStateOf(false) }
        TextButton(
            onClick = { yearPickerVisible = !yearPickerVisible },
            modifier = Modifier.height(CalendarDefaults.YearHeight),
            shape = CircleShape,
            colors = ButtonDefaults.textButtonColors(contentColor = LocalContentColor.current),
            elevation = null,
            border = null,
        ) {
            Text(
                text = "${selectedYear}年${selectedMonth}月",
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
            YearPicker(selectedYear, yearRange) {
                coroutineScope.launch {
                    pagerState.scrollToPage(pagerState.currentPage + (it - selectedYear) * 12)
                }
                yearPickerVisible = false
            }
        }
        WeekDays(dayNames)
        HorizontalPager(state = pagerState) { page ->
            //实现周和月数据的切换
            val monthDate = if (calendarState == CalendarDefaults.DragAnchors.WEEK) {
                var selectedPage = (selectedYear - firstYear) * 12 + selectedMonth - 1
                var monthDate = date[selectedPage]
                if (page == pagerState.targetPage && pagerState.targetPage - pagerState.currentPage > 0) {
                    var selectedWeek = monthDate.selectedWeek + 1
                    if (selectedWeek > monthDate.weeksInMonth - 1) {
                        selectedPage += 1
                        monthDate = date[selectedPage]
                        selectedWeek = -1
                        selectedYear = firstYear + selectedPage / 12
                        selectedMonth = selectedPage % 12 + 1
                        selectedDay = 1
                    }
                    monthDate.selectedWeek = selectedWeek
                } else if (page == pagerState.targetPage && pagerState.targetPage - pagerState.currentPage < 0) {
                    var selectedWeek = monthDate.selectedWeek - 1
                    if (selectedWeek < 0) {
                        selectedPage -= 1
                        monthDate = date[selectedPage]
                        selectedWeek = monthDate.weeksInMonth
                        selectedYear = firstYear + selectedPage / 12
                        selectedMonth = selectedPage % 12 + 1
                        selectedDay = 1
                    }
                    monthDate.selectedWeek = selectedWeek
                }
                monthDate
            } else {
                val monthDate = date[page]
                val y = firstYear + pagerState.currentPage / 12
                val m = pagerState.currentPage % 12 + 1
                if (y != selectedYear || m != selectedMonth) {
                    selectedYear = y
                    selectedMonth = m
                    selectedDay = 1
                }
                monthDate
            }
            val monthHeightDp = CalendarDefaults.WeekHeight * monthDate.weeksInMonth
            val anchoredDraggableState = remember(pagerState.currentPage) {
                AnchoredDraggableState(
                    initialValue = calendarState,
                    animationSpec = TweenSpec(durationMillis = 350),
                    anchors = DraggableAnchors {
                        CalendarDefaults.DragAnchors.WEEK at with(density) { CalendarDefaults.WeekHeight.toPx() }
                        CalendarDefaults.DragAnchors.MONTH at with(density) { monthHeightDp.toPx() }
                        CalendarDefaults.DragAnchors.MONTH_EXPAND at with(density) { monthExpandHeightDp.toPx() }
                    },
                    positionalThreshold = { distance -> distance * 0.5f },
                    velocityThreshold = { with(density) { 100.dp.toPx() } },
                )
            }
            var scrollEnabled by remember { mutableStateOf(false) }
            val listState = rememberLazyListState()
            LaunchedEffect(anchoredDraggableState) {
                snapshotFlow { anchoredDraggableState.currentValue }
                    .collectLatest {
                        calendarState = it
                        scrollEnabled =
                            it == CalendarDefaults.DragAnchors.WEEK && listState.canScrollForward
                        val monthPage = (selectedYear - firstYear) * 12 + selectedMonth - 1
                        //切换到正确的月份
                        if (it != CalendarDefaults.DragAnchors.WEEK && monthPage != pagerState.currentPage) {
                            pagerState.scrollToPage(monthPage)
                        }
                    }
            }

            Box(
                modifier = Modifier
                    .anchoredDraggable(
                        state = anchoredDraggableState,
                        orientation = Orientation.Vertical,
                    )
            ) {
                val monthOffset = with(density) { anchoredDraggableState.offset.toDp() }
                val monthExpand =
                    anchoredDraggableState.currentValue == CalendarDefaults.DragAnchors.MONTH_EXPAND
                Month(
                    monthDate,
                    monthOffset,
                    monthExpandHeightDp,
                    monthExpand
                ) { weekIndex, dayInfo ->
                    Day(
                        dayInfo,
                        monthExpand,
                        selectedMonth,
                        selectedDay
                    ) {
                        selectedYear = dayInfo.year
                        if (dayInfo.month == selectedMonth) {
                            selectedMonth = dayInfo.month
                            selectedDay = dayInfo.day
                            monthDate.selectedWeek = weekIndex
                        }
                    }
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(monthExpandHeightDp - monthOffset)
                        .offset(0.dp, monthOffset)
                        .background(colorResource(id = R.color.background))
                ) {
                    val selectedDate =
                        getLunarDate(selectedYear, selectedMonth, selectedDay)
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
                        LaunchedEffect(listState) {
                            snapshotFlow { listState.isScrollInProgress }
                                .collect {
                                    if (listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0) {
                                        scrollEnabled = false
                                    }
                                }
                        }
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
    monthDate: MonthInfo,
    monthOffset: Dp,
    monthExpandHeight: Dp,
    monthExpand: Boolean,
    content: @Composable BoxScope.(weekIndex: Int, dayInfo: DayInfo) -> Unit
) {
    val monthHeight = CalendarDefaults.WeekHeight * monthDate.weeksInMonth
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
        for (weekIndex in 0 until monthDate.weeksInMonth) {
            var zIndex = 0f
            val currWeekOffset = CalendarDefaults.WeekHeight * weekIndex
            var weekHeight = monthExpandHeight / monthDate.weeksInMonth
            val offsetY = if (monthOffset <= monthHeight) {
                weekHeight = CalendarDefaults.WeekHeight
                val offset = currWeekOffset + monthOffset - monthHeight
                if (monthDate.selectedWeek == weekIndex && offset < 0.dp) {
                    zIndex = 1f
                    0.dp
                } else {
                    zIndex = 0f
                    offset
                }
            } else {
                monthOffset / monthDate.weeksInMonth * weekIndex
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
                    if (monthDate.days.size == monthDate.weeksInMonth) {
                        Box(modifier = Modifier.weight(1f)) {
                            content(weekIndex, monthDate.days[weekIndex][dayIndex])
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun Day(
    dayInfo: DayInfo,
    monthExpand: Boolean,
    currMonth: Int,
    currDay: Int,
    onClick: () -> Unit = {}
) {
    val date = getLunarDate(dayInfo.year, dayInfo.month, dayInfo.day)
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
    val YearHeight = 45.dp
    val WeekHeight = 70.dp
    val DayHeight = 45.dp

    enum class DragAnchors {
        WEEK,
        MONTH,
        MONTH_EXPAND,
    }
}

data class MonthInfo(
    val year: Int,
    val month: Int,
    val days: MutableList<List<DayInfo>>,
    val firstDayOfMonth: Int,
    val weeksInMonth: Int,
    var selectedWeek: Int = 0,
)

data class DayInfo(
    val year: Int,
    val month: Int,
    val day: Int
)