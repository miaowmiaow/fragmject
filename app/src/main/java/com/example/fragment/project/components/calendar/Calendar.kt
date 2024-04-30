package com.example.fragment.project.components.calendar

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
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
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
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.layout.onSizeChanged
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fragment.project.R
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.math.max

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Calendar(
    viewModel: CalendarViewModel = viewModel(),
    hasCustomCalendar: (date: LunarDate) -> Boolean,
    contentPadding: PaddingValues = PaddingValues(10.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(10.dp),
    content: LazyListScope.(date: LunarDate) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()

    var calendarState by remember { mutableStateOf(CalendarDefaults.DragAnchors.MONTH) }

    var contentHeight by remember { mutableFloatStateOf(0f) }

    val localDate = LocalDate.now()

    var selectedYear by remember { mutableIntStateOf(localDate.year) }
    var selectedMonth by remember { mutableIntStateOf(localDate.monthValue) }
    var selectedDay by remember { mutableIntStateOf(localDate.dayOfMonth) }
    var selectedWeek by remember { mutableIntStateOf(0) }

    val weekInitialPage =
        uiState.monthMappingWeek["${selectedYear}-${selectedMonth}-${selectedWeek}"] ?: 0
    val weekPagerState =
        rememberCalendarPagerState(key1 = calendarState, weekInitialPage) { uiState.weekCount }
    val monthInitialPage = (selectedYear - uiState.yearRange.first) * 12 + selectedMonth - 1
    val monthPagerState =
        rememberCalendarPagerState(key1 = calendarState, monthInitialPage) { uiState.monthCount }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged {
                contentHeight = it.height.toFloat()
            }
    ) {
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
            YearPicker(selectedYear, uiState.yearRange) {
                selectedYear = it
                coroutineScope.launch {
                    if (calendarState == CalendarDefaults.DragAnchors.WEEK) {
                        val key = "${selectedYear}-${selectedMonth}-${selectedWeek}"
                        val page = uiState.monthMappingWeek[key] ?: 0
                        weekPagerState.scrollToPage(page)
                    } else {
                        val page = (selectedYear - uiState.yearRange.first) * 12 + selectedMonth - 1
                        monthPagerState.scrollToPage(page)
                    }
                }
                yearPickerVisible = false
            }
        }
        WeekDays(uiState.dayNames)
        val weekMode = calendarState == CalendarDefaults.DragAnchors.WEEK
        val state = if (weekMode) {
            weekPagerState
        } else {
            monthPagerState
        }
        HorizontalPager(state = state) { page ->
            val month = if (weekMode) {
                val pair = uiState.weekMappingMonth[page] ?: return@HorizontalPager
                val month = uiState.monthMap[pair.first] ?: return@HorizontalPager
                month.selectWeek = pair.second
                month
            } else {
                val key = "${uiState.yearRange.first + page / 12}-${page % 12 + 1}"
                val month = uiState.monthMap[key] ?: return@HorizontalPager
                month.selectWeek = selectedWeek
                month
            }
            HorizontalMonthsPage(
                month,
                selectedYear,
                selectedMonth,
                selectedDay,
                { y, m, d, w ->
                    selectedYear = y
                    selectedMonth = m
                    selectedDay = d
                    selectedWeek = w
                },
                contentHeight,
                calendarState,
                { state ->
                    calendarState = state
                },
                hasCustomCalendar,
                contentPadding,
                verticalArrangement,
                content
            )
            if (weekMode) {
                if (page == weekPagerState.currentPage && weekPagerState.currentPage != weekPagerState.targetPage) {
                    val pair = uiState.weekMappingMonth[page] ?: return@HorizontalPager
                    val ym = pair.first.split("-")
                    selectedYear = ym[0].toInt()
                    selectedMonth = ym[1].toInt()
                    selectedWeek = pair.second
                }
            } else {
                if (page == monthPagerState.targetPage && monthPagerState.currentPage != monthPagerState.targetPage) {
                    selectedYear = uiState.yearRange.first + page / 12
                    selectedMonth = page % 12 + 1
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
internal fun WeekDays(dayNames: List<Pair<String, String>>) {
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun HorizontalMonthsPage(
    month: Month,
    selectedYear: Int,
    selectedMonth: Int,
    selectedDay: Int,
    onSelectedDateChange: (selectedYear: Int, selectedMonth: Int, selectedDay: Int, selectedWeek: Int) -> Unit,
    contentHeight: Float,
    calendarState: CalendarDefaults.DragAnchors,
    onCalendarStateChange: (state: CalendarDefaults.DragAnchors) -> Unit,
    hasCustomCalendar: (date: LunarDate) -> Boolean,
    contentPadding: PaddingValues,
    verticalArrangement: Arrangement.Vertical,
    content: LazyListScope.(date: LunarDate) -> Unit
) {
    val density = LocalDensity.current
    val monthHeightDp = CalendarDefaults.WeekHeight * month.weeksInMonth()
    val contentHeightDp = with(density) {
        contentHeight.toDp()
    }
    val monthExpandHeightDp =
        contentHeightDp - CalendarDefaults.YearHeight - CalendarDefaults.DayHeight
    val anchoredDraggableState = remember(monthHeightDp, monthExpandHeightDp) {
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
    LaunchedEffect(anchoredDraggableState) {
        snapshotFlow { anchoredDraggableState.currentValue }.collectLatest {
            onCalendarStateChange(it)
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
            month,
            monthOffset,
            monthExpandHeightDp,
            monthExpand
        ) { weekIndex, date ->
            Day(
                date,
                selectedMonth,
                selectedDay,
                monthExpand
            ) {
                month.selectWeek = weekIndex
                onSelectedDateChange(date.year, date.month, date.day, weekIndex)
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(monthExpandHeightDp - monthOffset)
                .offset(0.dp, monthOffset)
                .background(colorResource(id = R.color.background))
        ) {
            val selectedDate = getLunarDate(selectedYear, selectedMonth, selectedDay)
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
                val listState = rememberLazyListState()
                val weekMode = calendarState == CalendarDefaults.DragAnchors.WEEK
                val scrollEnabled by remember { mutableStateOf(weekMode) }
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

@Composable
internal fun Month(
    monthDate: Month,
    monthOffset: Dp,
    monthExpandHeight: Dp,
    monthExpand: Boolean,
    content: @Composable BoxScope.(weekIndex: Int, date: Date) -> Unit
) {
    val monthHeight = CalendarDefaults.WeekHeight * monthDate.weeksInMonth()
    val animatedColor by animateColorAsState(
        if (monthExpand) colorResource(R.color.white) else colorResource(R.color.background),
        animationSpec = TweenSpec(350),
        label = "color"
    )
    Box(
        modifier = Modifier
            .background(colorResource(id = R.color.background))
            .fillMaxWidth()
            .requiredHeight(monthExpandHeight)
            .clipToBounds(),
    ) {
        for (weekIndex in 0 until monthDate.weeksInMonth()) {
            var zIndex = 0f
            val currWeekOffset = CalendarDefaults.WeekHeight * weekIndex
            var weekHeight = monthExpandHeight / monthDate.weeksInMonth()
            val offsetY = if (monthOffset <= monthHeight) {
                weekHeight = CalendarDefaults.WeekHeight
                val offset = currWeekOffset + monthOffset - monthHeight
                if (monthDate.selectWeek == weekIndex && offset < 0.dp) {
                    zIndex = 1f
                    0.dp
                } else {
                    zIndex = 0f
                    offset
                }
            } else {
                monthOffset / monthDate.weeksInMonth() * weekIndex
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .requiredHeight(weekHeight)
                    .offset(0.dp, offsetY)
                    .zIndex(zIndex)
                    .then(if (monthExpand) Modifier.padding(bottom = 1.dp) else Modifier)
                    .background(animatedColor),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.Top
            ) {
                for (dayIndex in 0 until CalendarDefaults.DAYS_IN_WEEK) {
                    if (monthDate.weeks.size == monthDate.weeksInMonth()) {
                        Box(modifier = Modifier.weight(1f)) {
                            content(weekIndex, monthDate.weeks[weekIndex][dayIndex])
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun Day(
    date: Date,
    currMonth: Int,
    currDay: Int,
    monthExpand: Boolean,
    onClick: () -> Unit = {}
) {
    Column {
        Column(modifier = Modifier
            .fillMaxWidth()
            .padding(1.dp)
            .clip(CircleShape)
            .clickable { onClick() }
            .then(
                if (date.isMonth && currDay == date.day) {
                    Modifier
                        .background(colorResource(R.color.theme_orange))
                        .border(1.dp, colorResource(R.color.theme_orange), CircleShape)
                } else {
                    Modifier
                }
            )
        ) {
            Text(
                text = date.day.toString(),
                modifier = Modifier
                    .fillMaxWidth()
                    .clipToBounds(),
                color = colorResource(if (date.isMonth) R.color.text_333 else R.color.text_999),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = "  ${
                    if (monthExpand) {
                        date.lunarDate().lunarDay
                    } else {
                        date.lunarDate().getFirstFestival()
                    }
                }  ",
                modifier = Modifier
                    .fillMaxWidth()
                    .clipToBounds(),
                color = colorResource(
                    if (currMonth == date.month && currDay == date.day) {
                        R.color.text_fff
                    } else if (date.lunarDate().isFestival() && !monthExpand) {
                        if (currMonth == date.month) {
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
            date.lunarDate().getFestival().forEach {
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

@ExperimentalFoundationApi
@Composable
fun rememberCalendarPagerState(
    key1: Any?,
    initialPage: Int = 0,
    initialPageOffsetFraction: Float = 0f,
    pageCount: () -> Int
): PagerState {
    return remember(key1) {
        CalendarPagerState(
            initialPage,
            initialPageOffsetFraction,
            pageCount
        )
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