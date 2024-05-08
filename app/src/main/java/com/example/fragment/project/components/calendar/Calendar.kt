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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.runtime.Immutable
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
    contentPadding: PaddingValues = PaddingValues(10.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(10.dp),
    customCalendar: (date: Date) -> List<String>?,
    content: LazyListScope.(date: Date) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()

    var calendarMode by remember { mutableStateOf(CalendarMode.Month) }
    var calendarHeight by remember { mutableFloatStateOf(0f) }

    val localDate = LocalDate.now()

    var currYear by remember { mutableIntStateOf(localDate.year) }
    var currMonth by remember { mutableIntStateOf(localDate.monthValue) }
    var currDay by remember { mutableIntStateOf(localDate.dayOfMonth) }
    var currWeek by remember {
        mutableIntStateOf(uiState.monthByDate(currYear, currMonth)?.selectedWeek ?: 0)
    }

    val weekInitialPage = uiState.weekIndexByDate(currYear, currMonth, currWeek)
    val weekPagerState =
        rememberCalendarPagerState(key1 = calendarMode, weekInitialPage) { uiState.weekCount }
    val monthInitialPage = (currYear - uiState.startYear()) * 12 + currMonth - 1
    val monthPagerState =
        rememberCalendarPagerState(key1 = calendarMode, monthInitialPage) { uiState.monthCount }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged {
                calendarHeight = it.height.toFloat()
            }
    ) {
        var yearPickerVisible by remember { mutableStateOf(false) }
        TextButton(
            onClick = { yearPickerVisible = !yearPickerVisible },
            modifier = Modifier.height(YearHeight),
            shape = CircleShape,
            colors = ButtonDefaults.textButtonColors(contentColor = LocalContentColor.current),
            elevation = null,
            border = null,
        ) {
            Text(
                text = "${currYear}年${currMonth}月",
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
            YearPicker(currYear, uiState.startYear(), uiState.lastYear()) {
                coroutineScope.launch {
                    if (calendarMode == CalendarMode.Week) {
                        val page = uiState.weekIndexByDate(currYear, currMonth, currWeek)
                        weekPagerState.scrollToPage(page)
                    } else {
                        val page = (currYear - uiState.startYear()) * 12 + currMonth - 1
                        monthPagerState.scrollToPage(page)
                    }
                }
                currYear = it
                yearPickerVisible = false
            }
        }
        WeekDays(uiState.dayNames)
        val pagerState = if (calendarMode == CalendarMode.Week) weekPagerState else monthPagerState
        LaunchedEffect(pagerState) {
            snapshotFlow { pagerState.currentPage }.collectLatest {
                if (calendarMode == CalendarMode.Week) {
                    currYear = uiState.yearByWeekIndex(it)
                    currMonth = uiState.monthByWeekIndex(it)
                    currWeek = uiState.weekByWeekIndex(it)
                } else {
                    currYear = uiState.startYear() + it / 12
                    currMonth = it % 12 + 1
                }
            }
        }
        HorizontalPager(state = pagerState) { page ->
            val month = if (calendarMode == CalendarMode.Week) {
                val month = uiState.monthByDate(
                    uiState.yearByWeekIndex(page),
                    uiState.monthByWeekIndex(page)
                ) ?: return@HorizontalPager
                month.selectedWeek = uiState.weekByWeekIndex(page)
                month
            } else {
                val month = uiState.monthByDate(
                    uiState.startYear() + page / 12,
                    page % 12 + 1
                ) ?: return@HorizontalPager
                month.selectedWeek = currWeek
                month
            }
            Month(
                calendarMode,
                calendarHeight,
                month,
                currYear,
                currMonth,
                currDay,
                { y, m, d, w ->
                    currYear = y
                    currMonth = m
                    currDay = d
                    currWeek = w
                },
                { mode ->
                    calendarMode = mode
                },
                contentPadding,
                verticalArrangement,
                customCalendar,
                content
            )
        }
    }
}


@Composable
internal fun YearPicker(
    currentYear: Int,
    startYear: Int,
    lastYear: Int,
    onSelectedYear: (year: Int) -> Unit
) {
    val yearsInRow = 3
    val lazyGridState = rememberLazyGridState(
        initialFirstVisibleItemIndex = max(
            0, currentYear - startYear - yearsInRow
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
        items(lastYear - startYear) {
            val localizedYear = it + startYear
            Text(
                text = localizedYear.toString(),
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable { onSelectedYear(localizedYear) }
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
                    .height(WeekDayHeight),
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun Month(
    calendarMode: CalendarMode,
    calendarHeight: Float,
    month: Month,
    currYear: Int,
    currMonth: Int,
    currDay: Int,
    onSelectedDateChange: (year: Int, month: Int, day: Int, week: Int) -> Unit,
    onCalendarStateChange: (mode: CalendarMode) -> Unit,
    contentPadding: PaddingValues,
    verticalArrangement: Arrangement.Vertical,
    customCalendar: (date: Date) -> List<String>?,
    content: LazyListScope.(date: Date) -> Unit
) {
    val density = LocalDensity.current
    val calendarHeightDp = with(density) {
        calendarHeight.toDp()
    }
    val weekModeHeight = WeekHeight
    val monthModeHeight = WeekHeight * month.weeksInMonth()
    val festivalModeHeight = calendarHeightDp - YearHeight - WeekDayHeight - VerticalArrowHeight
    val anchoredDraggableState = remember(monthModeHeight, festivalModeHeight) {
        AnchoredDraggableState(
            initialValue = calendarMode,
            animationSpec = TweenSpec(durationMillis = 350),
            anchors = DraggableAnchors {
                CalendarMode.Week at with(density) { weekModeHeight.toPx() }
                CalendarMode.Month at with(density) { monthModeHeight.toPx() }
                CalendarMode.Festival at with(density) { festivalModeHeight.toPx() }
            },
            positionalThreshold = { distance -> distance * 0.5f },
            velocityThreshold = { with(density) { 100.dp.toPx() } },
        )
    }
    var scrollEnabled by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }.collectLatest {
            if (listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0) {
                scrollEnabled = false
            }
        }
    }
    LaunchedEffect(anchoredDraggableState) {
        snapshotFlow { anchoredDraggableState.currentValue }.collectLatest {
            scrollEnabled = it == CalendarMode.Week && listState.canScrollForward
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
        val offset = with(density) { anchoredDraggableState.offset.toDp() }
        val festivalMode = anchoredDraggableState.currentValue == CalendarMode.Festival
        val animatedColor by animateColorAsState(
            if (festivalMode) colorResource(R.color.white) else colorResource(R.color.background),
            animationSpec = TweenSpec(350),
            label = "color"
        )
        Box(
            modifier = Modifier
                .background(colorResource(id = R.color.background))
                .fillMaxWidth()
                .requiredHeight(festivalModeHeight)
                .clipToBounds(),
        ) {
            for (weekIndex in 0 until month.weeksInMonth()) {
                var zIndex = 0f
                val weekOffset = WeekHeight * weekIndex
                var weekHeight = festivalModeHeight / month.weeksInMonth()
                val offsetY = if (offset <= monthModeHeight) {
                    weekHeight = WeekHeight
                    val currOffset = weekOffset + offset - monthModeHeight
                    if (month.selectedWeek == weekIndex && currOffset < 0.dp) {
                        zIndex = 1f
                        0.dp
                    } else {
                        zIndex = 0f
                        currOffset
                    }
                } else {
                    offset / month.weeksInMonth() * weekIndex
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .requiredHeight(weekHeight)
                        .offset(0.dp, offsetY)
                        .zIndex(zIndex)
                        .then(if (festivalMode) Modifier.padding(bottom = 1.dp) else Modifier)
                        .background(animatedColor),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.Top
                ) {
                    val week = month.weeks[weekIndex]
                    for (date in week) {
                        Box(modifier = Modifier.weight(1f)) {
                            Day(date, currMonth, currDay, festivalMode, customCalendar) {
                                month.selectedWeek = weekIndex
                                onSelectedDateChange(date.year, date.month, date.day, weekIndex)
                            }
                        }
                    }
                }
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .requiredHeight(calendarHeightDp - YearHeight - WeekDayHeight - WeekHeight)
                .offset(0.dp, offset)
                .background(colorResource(id = R.color.background))
        ) {
            val currDate = Date(currYear, currMonth, currDay, true)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .zIndex(1f),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(
                        id = when (calendarMode) {
                            CalendarMode.Week -> R.mipmap.ic_obtuse_bottom
                            CalendarMode.Festival -> R.mipmap.ic_obtuse_top
                            else -> R.mipmap.ic_line
                        }
                    ),
                    contentDescription = "",
                    modifier = Modifier
                        .height(VerticalArrowHeight)
                        .aspectRatio(1f)
                )
            }
            Text(
                text = currDate.lunar().lunarYear + " " + currDate.lunar().animalsYear + " " + currDate.lunar().lunarMonth + currDate.lunar().lunarDay,
                modifier = Modifier
                    .height(LunarHeight)
                    .padding(10.dp)
            )
            if (
                customCalendar(currDate).isNullOrEmpty()
                && currDate.lunar().getFestival().isEmpty()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .requiredHeight(calendarHeightDp - YearHeight - WeekDayHeight - VerticalArrowHeight - LunarHeight - offset),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Image(
                        painter = painterResource(R.mipmap.ic_calendar),
                        contentDescription = null,
                        modifier = Modifier.size(65.dp)
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
                    content(currDate)
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
    festivalMode: Boolean,
    customCalendar: (date: Date) -> List<String>?,
    onClick: () -> Unit = {}
) {
    Column {
        Column(modifier = Modifier
            .fillMaxWidth()
            .padding(1.dp)
            .clip(CircleShape)
            .clickable {
                if (date.isMonth) {
                    onClick()
                }
            }
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
                    if (festivalMode) {
                        date.lunar().lunarDay
                    } else {
                        date.lunar().getFirstFestival()
                    }
                }  ",
                modifier = Modifier
                    .fillMaxWidth()
                    .clipToBounds(),
                color = colorResource(
                    if (date.isMonth && currDay == date.day) {
                        R.color.text_fff
                    } else if (date.lunar().isFestival() && !festivalMode) {
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
        customCalendar(date)?.forEach {
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
        if (festivalMode) {
            date.lunar().getFestival().forEach {
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

internal val YearHeight = 45.dp
internal val WeekDayHeight = 45.dp
internal val WeekHeight = 70.dp
internal val LunarHeight = 40.dp
internal val VerticalArrowHeight = 25.dp

@Immutable
@JvmInline
value class CalendarMode internal constructor(internal val value: Int) {

    companion object {
        val Week = CalendarMode(0)
        val Month = CalendarMode(1)
        val Festival = CalendarMode(2)
    }

    override fun toString() = when (this) {
        Week -> "Week"
        Month -> "Month"
        Festival -> "Festival"
        else -> "Unknown"
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