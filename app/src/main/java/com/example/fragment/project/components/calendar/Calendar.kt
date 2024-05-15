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
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.lazy.LazyListState
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.zIndex
import com.example.fragment.project.R
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.max

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Calendar(
    schedules: MutableList<CalendarSchedule>,
    modifier: Modifier = Modifier,
    onSelectedDateChange: (year: Int, month: Int, day: Int) -> Unit,
    content: LazyListScope.(date: CalendarDate) -> Unit
) {
    val defaultLocale = LocalConfiguration.current.locales[0]
    val calendarModel = remember(defaultLocale) { CalendarModel(defaultLocale) }

    calendarModel.updateSchedule(schedules)

    val weekModePagerState = rememberCalendarPagerState(
        initialPage = calendarModel.weekModeIndexByDate(
            calendarModel.localYear,
            calendarModel.localMonth
        )
    ) { calendarModel.weekModeCount() }

    val monthModePagerState = rememberCalendarPagerState(
        initialPage = (calendarModel.localYear - calendarModel.startYear()) * 12 + calendarModel.localMonth - 1
    ) { calendarModel.monthModeCount() }

    BoxWithConstraints(modifier = modifier) {
        val calendarHeight = maxHeight
        var calendarMode by remember { mutableStateOf(CalendarMode.Week) }
        Column(modifier = Modifier.fillMaxSize()) {
            YearPicker(
                calendarModel = calendarModel,
                calendarMode = calendarMode,
                weekModePagerState = weekModePagerState,
                monthModePagerState = monthModePagerState,
            )
            WeekDays(calendarModel.dayNames)
            MonthPager(
                calendarHeight = calendarHeight,
                calendarModel = calendarModel,
                calendarMode = calendarMode,
                weekModePagerState = weekModePagerState,
                monthModePagerState = monthModePagerState,
                onCalendarStateChange = { mode -> calendarMode = mode },
                onSelectedDateChange = onSelectedDateChange,
                content = content
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun YearPicker(
    calendarModel: CalendarModel,
    calendarMode: CalendarMode,
    weekModePagerState: PagerState,
    monthModePagerState: PagerState,
) {
    val coroutineScope = rememberCoroutineScope()
    val weekMode = calendarMode == CalendarMode.Week
    val selectedYear = if (weekMode) {
        calendarModel.yearByWeekModeIndex(weekModePagerState.currentPage)
    } else {
        calendarModel.startYear() + monthModePagerState.currentPage / 12
    }
    val selectedMonth = if (weekMode) {
        calendarModel.monthByWeekModeIndex(weekModePagerState.currentPage)
    } else {
        monthModePagerState.currentPage % 12 + 1
    }
    Column {
        var pickerVisible by remember { mutableStateOf(false) }
        TextButton(
            onClick = { pickerVisible = !pickerVisible },
            modifier = Modifier.height(YearHeight),
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
                Modifier.rotate(if (pickerVisible) 180f else 0f)
            )
        }
        AnimatedVisibility(
            visible = pickerVisible,
            modifier = Modifier.clipToBounds(),
            enter = expandVertically() + fadeIn(initialAlpha = 0.6f),
            exit = shrinkVertically() + fadeOut()
        ) {
            val yearsInRow = 3
            val lazyGridState = rememberLazyGridState(
                initialFirstVisibleItemIndex = max(
                    0, selectedYear - calendarModel.startYear() - yearsInRow
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
                items(calendarModel.lastYear() - calendarModel.startYear()) {
                    val localizedYear = it + calendarModel.startYear()
                    Text(
                        text = localizedYear.toString(),
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable {
                                coroutineScope.launch {
                                    if (weekMode) {
                                        val page = calendarModel.weekModeIndexByDate(
                                            localizedYear,
                                            selectedMonth
                                        )
                                        weekModePagerState.scrollToPage(page)
                                    } else {
                                        val page = it * 12 + selectedMonth - 1
                                        monthModePagerState.scrollToPage(page)
                                    }
                                }
                                pickerVisible = false
                            }
                            .background(
                                colorResource(if (localizedYear == selectedYear) R.color.theme else R.color.transparent),
                                RoundedCornerShape(50)
                            ),
                        color = colorResource(if (localizedYear == selectedYear) R.color.text_fff else R.color.text_333),
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
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
internal fun MonthPager(
    calendarHeight: Dp,
    calendarModel: CalendarModel,
    calendarMode: CalendarMode,
    monthModePagerState: PagerState,
    weekModePagerState: PagerState,
    onCalendarStateChange: (mode: CalendarMode) -> Unit,
    onSelectedDateChange: (year: Int, month: Int, day: Int) -> Unit,
    content: LazyListScope.(date: CalendarDate) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val weekMode = calendarMode == CalendarMode.Week
    var selectedDate by remember {
        mutableStateOf(
            calendarModel.calendarDate(
                calendarModel.localYear,
                calendarModel.localMonth,
                calendarModel.localDay
            )
        )
    }
    LaunchedEffect(selectedDate.year, selectedDate.month, selectedDate.day) {
        onSelectedDateChange(selectedDate.year, selectedDate.month, selectedDate.day)
    }
    val pagerState = if (weekMode) weekModePagerState else monthModePagerState
    //周模式和月模式联动
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collectLatest { page ->
            if (calendarMode == CalendarMode.Week) {
                val month = calendarModel.weekModeByIndex(page) ?: return@collectLatest
                val selectedWeek = calendarModel.weekByWeekModeIndex(page)
                val isDate = month.weeks[selectedWeek].firstOrNull { it.isDay }
                if (isDate == null) {
                    selectedDate.isDay = false
                    val date = month.weeks[selectedWeek].first { it.isMonth }
                    date.isDay = true
                    selectedDate = date
                }
                val p = (month.year - calendarModel.startYear()) * 12 + month.month - 1
                calendarModel.monthModeByIndex(p)?.let {
                    it.selectedWeek = selectedWeek
                }
                if (monthModePagerState.currentPage != p) {
                    coroutineScope.launch {
                        monthModePagerState.scrollToPage(p)
                    }
                }
            } else {
                val month = calendarModel.monthModeByIndex(page) ?: return@collectLatest
                val isDate = month.weeks[month.selectedWeek].firstOrNull { it.isDay }
                if (isDate == null) {
                    val selectedMonth =
                        calendarModel.monthModeByDate(selectedDate.year, selectedDate.month)
                    selectedMonth?.selectedWeek = 0
                    selectedDate.isDay = false
                    val date = month.weeks[month.selectedWeek].first { it.isMonth }
                    date.isDay = true
                    selectedDate = date
                }
                val p = calendarModel.weekModeIndexByDate(month.year, month.month)
                if (weekModePagerState.currentPage != p) {
                    coroutineScope.launch {
                        weekModePagerState.scrollToPage(p)
                    }
                }
            }
        }
    }
    HorizontalPager(state = pagerState) { page ->
        val calendarMonth = if (weekMode) {
            calendarModel.weekModeByIndex(page)
        } else {
            calendarModel.monthModeByIndex(page)
        } ?: return@HorizontalPager
        val weekModeHeight = with(density) { WeekHeight.toPx() }
        val monthModeHeight = with(density) { (WeekHeight * calendarMonth.weeksInMonth()).toPx() }
        val festivalModeHeight =
            with(density) { (calendarHeight - YearHeight - WeekDayHeight - VerticalArrowHeight).toPx() }
        val anchoredDraggableState = remember(monthModeHeight, festivalModeHeight) {
            AnchoredDraggableState(
                initialValue = calendarMode,
                anchors = DraggableAnchors {
                    CalendarMode.Week at weekModeHeight
                    CalendarMode.Month at monthModeHeight
                    CalendarMode.Festival at festivalModeHeight
                },
                positionalThreshold = { distance -> distance * 0.5f },
                velocityThreshold = { with(density) { 100.dp.toPx() } },
                animationSpec = TweenSpec(durationMillis = 350),
            )
        }
        LaunchedEffect(calendarModel) {
            if (calendarModel.firstStartup) {
                anchoredDraggableState.animateTo(CalendarMode.Month)
                calendarModel.firstStartup = false
            }
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
                .background(colorResource(id = R.color.background))
                .fillMaxSize()
                .clipToBounds(),
        ) {
            val offset = anchoredDraggableState.requireOffset()
            val festivalMode = anchoredDraggableState.currentValue == CalendarMode.Festival
            val animatedColor by animateColorAsState(
                if (festivalMode) colorResource(R.color.white) else colorResource(R.color.background),
                animationSpec = TweenSpec(350),
                label = "color"
            )
            for (weekIndex in 0 until calendarMonth.weeksInMonth()) {
                var zIndex = 0f
                val weekOffset = weekModeHeight * weekIndex
                var weekHeight = festivalModeHeight / calendarMonth.weeksInMonth()
                val offsetY = if (offset <= monthModeHeight) {
                    weekHeight = weekModeHeight
                    val currOffset = weekOffset + offset - monthModeHeight
                    if (calendarMonth.selectedWeek == weekIndex && currOffset < 0) {
                        zIndex = 1f
                        0
                    } else {
                        zIndex = 0f
                        currOffset
                    }
                } else {
                    offset / calendarMonth.weeksInMonth() * weekIndex
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .requiredHeight(with(density) { weekHeight.toDp() })
                        .offset { IntOffset(x = 0, y = offsetY.toInt()) }
                        .zIndex(zIndex)
                        .then(if (festivalMode) Modifier.padding(bottom = 1.dp) else Modifier)
                        .background(animatedColor),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.Top
                ) {
                    val week = calendarMonth.weeks[weekIndex]
                    for (date in week) {
                        Box(modifier = Modifier.weight(1f)) {
                            Day(date, festivalMode) {
                                selectedDate.isDay = false
                                date.isDay = true
                                if (calendarMode != CalendarMode.Week) {
                                    calendarMonth.selectedWeek = weekIndex
                                    val p = calendarModel.weekModeIndexByDate(
                                        calendarMonth.year,
                                        calendarMonth.month
                                    )
                                    if (weekModePagerState.currentPage != p) {
                                        coroutineScope.launch {
                                            weekModePagerState.scrollToPage(p)
                                        }
                                    }
                                }
                                selectedDate = date
                            }
                        }
                    }
                }
            }
            Schedule(
                calendarHeight = calendarHeight,
                calendarMode = calendarMode,
                calendarDate = selectedDate,
                scrollProvider = { offset.toInt() },
                listState = listState,
                scrollEnabled = scrollEnabled
            ) {
                content(it)
            }
        }
    }
}

@Composable
internal fun Day(
    calendarDate: CalendarDate,
    festivalMode: Boolean,
    onClick: () -> Unit = {}
) {
    Column {
        Column(modifier = Modifier
            .padding(1.dp)
            .aspectRatio(1f)
            .clip(CircleShape)
            .clickable {
                if (calendarDate.isMonth) {
                    onClick()
                }
            }
            .then(
                if (calendarDate.isMonth && calendarDate.isDay) {
                    Modifier
                        .background(colorResource(R.color.theme_orange))
                        .border(1.dp, colorResource(R.color.theme_orange), CircleShape)
                } else {
                    Modifier
                }
            )
        ) {
            Text(
                text = calendarDate.day.toString(),
                modifier = Modifier
                    .fillMaxWidth()
                    .clipToBounds(),
                color = colorResource(if (calendarDate.isMonth) R.color.text_333 else R.color.text_999),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = "  ${
                    if (festivalMode) {
                        calendarDate.lunarDay()
                    } else {
                        calendarDate.getFirstFestival()
                    }
                }  ",
                modifier = Modifier
                    .fillMaxWidth()
                    .clipToBounds(),
                color = colorResource(
                    if (calendarDate.isMonth && calendarDate.isDay) {
                        R.color.text_fff
                    } else if (calendarDate.isFestival() && !festivalMode) {
                        if (calendarDate.isMonth) {
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
        calendarDate.getSchedule().forEach {
            Festival(it)
        }
        if (festivalMode) {
            calendarDate.getFestival().forEach {
                Festival(it)
            }
        }
    }
}

@Composable
internal fun Festival(text: String) {
    Text(
        text = text,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 3.dp, vertical = 1.dp)
            .background(
                colorResource(R.color.gray_e5),
                RoundedCornerShape(3.dp)
            ),
        color = colorResource(R.color.text_999),
        fontSize = 10.sp,
        textAlign = TextAlign.Center,
        lineHeight = 14.sp,
        overflow = TextOverflow.Ellipsis,
        maxLines = 1,
    )
}

@Composable
internal fun Schedule(
    calendarHeight: Dp,
    calendarMode: CalendarMode,
    calendarDate: CalendarDate,
    scrollProvider: () -> Int,
    listState: LazyListState,
    scrollEnabled: Boolean,
    content: LazyListScope.(date: CalendarDate) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .requiredHeight(calendarHeight - YearHeight - WeekDayHeight - WeekHeight)
            .offset { IntOffset(x = 0, y = scrollProvider()) }
            .background(colorResource(id = R.color.background))
    ) {
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
            text = calendarDate.lunarYear() + " " + calendarDate.animalsYear() + " " + calendarDate.lunarMonth() + calendarDate.lunarDay(),
            modifier = Modifier
                .height(LunarHeight)
                .padding(10.dp)
        )
        if (calendarDate.getSchedule().isEmpty() && calendarDate.getFestival().isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .requiredHeight(
                        calendarHeight - YearHeight - WeekDayHeight - VerticalArrowHeight - LunarHeight - with(
                            LocalDensity.current
                        ) { scrollProvider().toDp() }),
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
                contentPadding = PaddingValues(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                userScrollEnabled = scrollEnabled
            ) {
                content(calendarDate)
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