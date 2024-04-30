package com.example.fragment.project.components.calendar

data class Month(
    val year: Int,
    val month: Int,
    val weeks: MutableList<List<Date>>,
    var selectWeek: Int = 0,
) {
    fun weeksInMonth(): Int {
        return weeks.size
    }
}

data class Date(
    val year: Int,
    val month: Int,
    val day: Int,
    val isMonth: Boolean,
) {
    fun lunarDate(): LunarDate {
        return getLunarDate(year, month, day)
    }
}