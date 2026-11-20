package com.example.fragmject.feature.demo.ui.calendar

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fragmject.core.domain.repository.ScheduleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate
import javax.inject.Inject

/**
 * 日历示例页的 ViewModel：日程数据与选中日期的唯一状态持有者。
 *
 * 通过 [ScheduleRepository] 按日读写日程；日程状态以 [LocalDate] 为 key 聚合，
 * 选中态由 [selectedDate] 表达，取代原先散落在每个 CalendarDate 上的
 * MutableStateFlow，使日期模型只承载展示数据、读写生命周期收敛到本 VM。
 */
@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val scheduleRepository: ScheduleRepository,
) : ViewModel() {

    private val _scheduleMap = MutableStateFlow<Map<LocalDate, List<String>>>(emptyMap())
    val scheduleMap: StateFlow<Map<LocalDate, List<String>>> = _scheduleMap.asStateFlow()

    private val _selectedDate = MutableStateFlow<LocalDate?>(null)
    val selectedDate: StateFlow<LocalDate?> = _selectedDate.asStateFlow()

    private val scheduleMutex = Mutex()

    init {
        val today = LocalDate.now()
        _selectedDate.value = today
        loadSchedule(today)
    }

    fun selectDate(date: LocalDate) {
        if (_selectedDate.value == date) return
        _selectedDate.value = date
        loadSchedule(date)
    }

    fun loadSchedule(date: LocalDate) {
        viewModelScope.launch {
            scheduleMutex.withLock {
                val list = scheduleRepository.getSchedule(date.year, date.monthValue, date.dayOfMonth)
                _scheduleMap.update { it + (date to list) }
            }
        }
    }

    fun addSchedule(date: LocalDate, text: String) {
        updateSchedule(date) { it + text }
    }

    fun removeSchedule(date: LocalDate, text: String) {
        updateSchedule(date) { it.toMutableList().apply { remove(text) } }
    }

    private fun updateSchedule(date: LocalDate, transform: (List<String>) -> List<String>) {
        viewModelScope.launch {
            scheduleMutex.withLock {
                val previous = _scheduleMap.value[date]?.toList()
                val updated = transform(previous.orEmpty())
                _scheduleMap.update { it + (date to updated) }
                var persisted = false
                try {
                    scheduleRepository.setSchedule(date.year, date.monthValue, date.dayOfMonth, updated)
                    persisted = true
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e("CalendarViewModel", "setSchedule failed", e)
                } finally {
                    if (!persisted) {
                        _scheduleMap.update { current ->
                            if (previous == null) current - date else current + (date to previous)
                        }
                    }
                }
            }
        }
    }
}
