package com.example.fragmject.feature.demo.ui.calendar

import androidx.compose.runtime.Stable
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * 周/月两个 Pager 的联动同步与日期选中转发。
 *
 * 原实现内联在 `MonthPager` 的 `LaunchedEffect` 与日期点击里，分支堆叠使该组合函数
 * 圈复杂度与行数长期超阈值；抽成独立类后两个方向的同步各自成函数，页面侧只剩转发。
 */
@Stable
internal class CalendarPagerSynchronizer(
    private val model: CalendarModel,
    private val pagers: CalendarPagerStates,
    private val callbacks: CalendarDateCallbacks,
    private val onSelectedWeek: (Int) -> Unit,
) {

    /**
     * 当前 Pager 翻页后，把选中周/选中日与另一个 Pager 的页码对齐。
     *
     * @param selectedWeek 当前选中的周序号（月模式下的入口值）
     */
    suspend fun sync(
        page: Int,
        isWeekMode: Boolean,
        selectedDate: LocalDate?,
        selectedWeek: Int,
        scope: CoroutineScope,
    ) {
        if (isWeekMode) {
            syncFromWeekPager(page, selectedDate, scope)
        } else {
            syncFromMonthPager(page, selectedDate, selectedWeek, scope)
        }
    }

    /** 点击某天：写回选中日期与周序号，月模式下载把周 Pager 对齐到该周。 */
    fun selectDate(date: CalendarDate, mode: CalendarMode, scope: CoroutineScope) {
        callbacks.onSelectDate(date.localDate)
        callbacks.onSelectedDateChange(date.year, date.month, date.day)
        onSelectedWeek(date.week)
        if (mode == CalendarMode.Week) return
        scrollWeekPagerTo(date.year, date.month, date.week, scope)
    }

    /** 周模式翻页：选中该页对应的周，必要时月 Pager 跳到同一月份。 */
    private fun syncFromWeekPager(
        page: Int,
        selectedDate: LocalDate?,
        scope: CoroutineScope,
    ) {
        val month = model.weekModeByIndex(page) ?: return
        val week = model.weekByWeekModeIndex(page)
        onSelectedWeek(week)
        val selected = month.weeks[week].firstOrNull { it.localDate == selectedDate }
        if (selected == null) {
            month.weeks[week].firstOrNull { it.currMonth }?.localDate?.let(callbacks.onSelectDate)
        }
        scrollMonthPagerTo(month, scope)
    }

    /** 月模式翻页：保持周序号（越界则回到第 0 周），周 Pager 跳到同一周。 */
    private fun syncFromMonthPager(
        page: Int,
        selectedDate: LocalDate?,
        selectedWeek: Int,
        scope: CoroutineScope,
    ) {
        val month = model.monthModeByIndex(page) ?: return
        var week = selectedWeek
        val selected = month.weeks[week].firstOrNull { it.localDate == selectedDate }
        if (selected == null) {
            week = 0
            onSelectedWeek(week)
            month.weeks[week].firstOrNull { it.currMonth }?.localDate?.let(callbacks.onSelectDate)
        }
        scrollWeekPagerTo(month.year, month.month, week, scope)
    }

    /** 月 Pager 未停在同一月份页时才滚动，避免重复跳页打断手势。 */
    private fun scrollMonthPagerTo(month: CalendarMonth, scope: CoroutineScope) {
        val index = (month.year - model.startYear()) * 12 + month.month - 1
        if (pagers.monthModePagerState.currentPage != index) {
            scope.launch { pagers.monthModePagerState.scrollToPage(index) }
        }
    }

    /** 周 Pager 未停在同一周页时才滚动，避免重复跳页打断手势。 */
    private fun scrollWeekPagerTo(year: Int, month: Int, week: Int, scope: CoroutineScope) {
        val index = model.weekModeIndexByDate(year, month, week)
        if (pagers.weekModePagerState.currentPage != index) {
            scope.launch { pagers.weekModePagerState.scrollToPage(index) }
        }
    }
}
