package com.example.fragment.project.ui.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fragment.project.R
import com.example.fragment.project.WanTheme
import com.example.fragment.project.components.calendar.Calendar
import com.example.fragment.project.components.calendar.CalendarSchedule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class CalendarUiState(
    var schedules: MutableList<CalendarSchedule> = mutableListOf(
        CalendarSchedule(
            2024,
            4,
            18,
            mutableListOf(
                "11",
                "22",
                "33",
                "11",
                "22",
                "33",
                "11",
                "22",
                "33",
                "11",
                "22",
                "33",
                "11",
                "22",
                "33"
            )
        ),
        CalendarSchedule(2024, 4, 19, mutableListOf("aa", "bb", "cc")),
    ),
    private val updateTime: Long = 0
)

class CalendarViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    fun updateSchedule(year: Int, month: Int, day: Int, strs: MutableList<String>) {
        _uiState.update { state ->
            state.schedules
                .associateBy({ "${it.year}-${it.month}-${it.day}" }, { it.schedules })
                .getOrElse("${year}-${month}-${day}") { mutableListOf() }
                .apply {
                    if (isEmpty()) {
                        state.schedules.add(CalendarSchedule(year, month, day, strs))
                    } else {
                        this.clear()
                        this.addAll(strs)
                    }
                }
            state.copy(updateTime = System.nanoTime())
        }
    }
}

@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var selectedYear = 0
    var selectedMonth = 0
    var selectedDay = 0

    Box(contentAlignment = Alignment.BottomEnd) {
        Calendar(
            schedules = uiState.schedules,
            modifier = Modifier.padding(vertical = 15.dp),
            onSelectedDateChange = { year, month, day ->
                selectedYear = year
                selectedMonth = month
                selectedDay = day
            }
        ) { date ->
            itemsIndexed(
                items = date.getSchedule(),
            ) { _, item ->
                Column(
                    modifier = Modifier
                        .background(colorResource(R.color.white), RoundedCornerShape(10.dp))
                        .fillMaxWidth()
                        .padding(10.dp)
                ) {
                    Text(text = item)
                    Text(text = "${date.year}年${date.month}月${date.day}日")
                }
            }
            itemsIndexed(
                items = date.getFestival(),
            ) { _, item ->
                Column(
                    modifier = Modifier
                        .background(colorResource(R.color.white), RoundedCornerShape(10.dp))
                        .fillMaxWidth()
                        .padding(10.dp)
                ) {
                    Text(text = item)
                    Text(text = "${date.year}年${date.month}月${date.day}日")
                }
            }
        }
        Button(
            onClick = {
                viewModel.updateSchedule(
                    selectedYear,
                    selectedMonth,
                    selectedDay,
                    mutableListOf("qq", "ww", "ee")
                )
            },
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(
                containerColor = colorResource(R.color.theme_orange),
                contentColor = colorResource(R.color.white)
            ),
            elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp),
            contentPadding = PaddingValues(15.dp),
            modifier = Modifier
                .padding(15.dp)
                .size(55.dp)
        ) {
            Icon(
                painter = painterResource(R.mipmap.ic_add),
                contentDescription = null
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF0F0F0)
@Composable
fun CalendarScreenPreview() {
    WanTheme { CalendarScreen() }
}