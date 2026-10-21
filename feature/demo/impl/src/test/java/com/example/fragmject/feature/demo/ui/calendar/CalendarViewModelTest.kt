package com.example.fragmject.feature.demo.ui.calendar

import com.example.fragmject.core.domain.repository.ScheduleRepository
import com.example.fragmject.core.testing.MainDispatcherRule
import java.io.IOException
import java.time.LocalDate
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * [CalendarViewModel] 测试：选中日期驱动的日程加载，以及新增/删除的乐观更新与回滚。
 *
 * 乐观更新失败必须回滚到操作前的快照，否则 UI 会显示一条实际未落盘的日程。
 */
class CalendarViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `init - selects today and loads its schedule`() = runTest {
        val today = LocalDate.now()
        val repo = FakeScheduleRepository(seed = mapOf(today to listOf("站会")))

        val viewModel = CalendarViewModel(repo)

        assertEquals(today, viewModel.selectedDate.value)
        assertEquals(listOf("站会"), viewModel.scheduleMap.value[today])
    }

    @Test
    fun `select date - loads schedule for the date`() = runTest {
        val date = LocalDate.of(2026, 5, 1)
        val repo = FakeScheduleRepository(seed = mapOf(date to listOf("放假")))

        val viewModel = CalendarViewModel(repo)
        viewModel.selectDate(date)

        assertEquals(date, viewModel.selectedDate.value)
        assertEquals(listOf("放假"), viewModel.scheduleMap.value[date])
    }

    @Test
    fun `select same date - does not reload`() = runTest {
        val date = LocalDate.of(2026, 5, 1)
        val repo = FakeScheduleRepository()
        val viewModel = CalendarViewModel(repo)

        viewModel.selectDate(date)
        viewModel.selectDate(date)

        assertEquals(1, repo.getCalls.count { it == date })
    }

    @Test
    fun `add schedule - appends and persists`() = runTest {
        val date = LocalDate.of(2026, 5, 1)
        val repo = FakeScheduleRepository(seed = mapOf(date to listOf("站会")))
        val viewModel = CalendarViewModel(repo)
        // 先选中该日期，让 VM 从仓储把"已有日程"读进 scheduleMap，增量才有基线
        viewModel.selectDate(date)

        viewModel.addSchedule(date, "评审")

        assertEquals(listOf("站会", "评审"), viewModel.scheduleMap.value[date])
        assertEquals(listOf("站会", "评审"), repo.setCalls.single().second)
    }

    @Test
    fun `add schedule persistence failure - rolls back`() = runTest {
        val date = LocalDate.of(2026, 5, 1)
        val repo = FakeScheduleRepository(seed = mapOf(date to listOf("站会")))
        val viewModel = CalendarViewModel(repo)
        viewModel.selectDate(date)
        repo.failOnSet = true

        viewModel.addSchedule(date, "评审")

        assertEquals("落盘失败必须回滚，避免展示未保存的日程", listOf("站会"), viewModel.scheduleMap.value[date])
    }

    @Test
    fun `remove schedule - removes and persists`() = runTest {
        val date = LocalDate.of(2026, 5, 1)
        val repo = FakeScheduleRepository(seed = mapOf(date to listOf("站会", "评审")))
        val viewModel = CalendarViewModel(repo)
        viewModel.selectDate(date)

        viewModel.removeSchedule(date, "站会")

        assertEquals(listOf("评审"), viewModel.scheduleMap.value[date])
        assertEquals(listOf("评审"), repo.setCalls.single().second)
    }

    @Test
    fun `remove schedule persistence failure - rolls back`() = runTest {
        val date = LocalDate.of(2026, 5, 1)
        val repo = FakeScheduleRepository(seed = mapOf(date to listOf("站会", "评审")))
        val viewModel = CalendarViewModel(repo)
        viewModel.selectDate(date)
        repo.failOnSet = true

        viewModel.removeSchedule(date, "站会")

        assertEquals(listOf("站会", "评审"), viewModel.scheduleMap.value[date])
    }
}

private class FakeScheduleRepository(
    seed: Map<LocalDate, List<String>> = emptyMap(),
) : ScheduleRepository {

    private val store = seed.toMutableMap()
    var failOnSet = false

    val getCalls = mutableListOf<LocalDate>()
    val setCalls = mutableListOf<Pair<LocalDate, List<String>>>()

    override suspend fun setSchedule(year: Int, month: Int, day: Int, list: List<String>) {
        if (failOnSet) throw IOException("disk full")
        val date = LocalDate.of(year, month, day)
        store[date] = list
        setCalls += date to list
    }

    override suspend fun getSchedule(year: Int, month: Int, day: Int): List<String> {
        val date = LocalDate.of(year, month, day)
        getCalls += date
        return store[date].orEmpty()
    }
}
