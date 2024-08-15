package com.example.fragment.project.components.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.fragment.project.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun DayContent(
    date: CalendarDate,
    isMonthFillMode: Boolean,
    onClick: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val scheduleState by date.scheduleState.collectAsStateWithLifecycle()
    var isDay by remember { mutableStateOf(date.isDay) }
    LaunchedEffect(date.dayState) {
        date.dayState?.handleDayEvents(
            onSchedule = { text ->
                scope.launch {
                    date.addSchedule(text)
                }
            },
            onSelected = {
                isDay = it
                date.isDay = isDay
            }
        )
    }
    Column {
        Column(modifier = Modifier
            .padding(1.dp)
            .aspectRatio(1f)
            .clip(CircleShape)
            .clickable {
                if (date.isMonth) {
                    onClick()
                }
            }
            .then(
                if (date.isMonth && isDay) {
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
                    if (isMonthFillMode) {
                        date.lunarDay()
                    } else {
                        date.getFirstFestival()
                    }
                }  ",
                modifier = Modifier
                    .fillMaxWidth()
                    .clipToBounds(),
                color = colorResource(
                    if (date.isMonth && isDay) {
                        R.color.text_fff
                    } else if (date.isMonth && date.isFestival() && !isMonthFillMode) {
                        R.color.theme_orange
                    } else if (!date.isMonth && date.isFestival() && !isMonthFillMode) {
                        R.color.b_zero_theme_orange
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
        scheduleState.forEach {
            Text(
                text = it,
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
        if (isMonthFillMode) {
            date.getFestival().forEach {
                Text(
                    text = it,
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
        }
    }
}

@Stable
class DayState(
    private val coroutineScope: CoroutineScope
) {
    private sealed interface DayEvent {
        data class Schedule(val text: String) : DayEvent
        data class Selected(val selected: Boolean) : DayEvent
    }

    private val dayEvents: MutableSharedFlow<DayEvent> = MutableSharedFlow()

    internal suspend fun handleDayEvents(
        onSchedule: (String) -> Unit = {},
        onSelected: (Boolean) -> Unit = {},
    ): Nothing = withContext(Dispatchers.Main) {
        dayEvents.collect { event ->
            when (event) {
                is DayEvent.Schedule -> onSchedule(event.text)
                is DayEvent.Selected -> onSelected(event.selected)
            }
        }
    }

    fun onSchedule(text: String) {
        coroutineScope.launch { dayEvents.emit(DayEvent.Schedule(text)) }
    }

    fun onSelected(selected: Boolean) {
        coroutineScope.launch { dayEvents.emit(DayEvent.Selected(selected)) }
    }

}

@Composable
fun rememberDayState(
    coroutineScope: CoroutineScope = rememberCoroutineScope()
): DayState = remember(coroutineScope) { DayState(coroutineScope) }
