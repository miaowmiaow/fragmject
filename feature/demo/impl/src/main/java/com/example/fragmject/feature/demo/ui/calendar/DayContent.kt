package com.example.fragmject.feature.demo.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

@Composable
internal fun DayContent(
    date: CalendarDate,
    isMonthFillMode: Boolean,
    schedule: List<String>,
    isSelected: Boolean,
    onClick: () -> Unit = {}
) {
    val festivals = remember(date) { date.getFestival() }
    val hasFestival = remember(date) { date.isFestival() }
    val dayLabel = remember(date, isMonthFillMode) {
        if (isMonthFillMode) {
            date.lunarDay()
        } else {
            date.getFirstFestival()
        }
    }
    val schedulePreview = remember(schedule) { schedule.take(2) }
    val festivalPreview = remember(festivals) { festivals.take(2) }

    Column {
        DayCircle(
            date = date,
            dayLabel = dayLabel,
            isSelected = isSelected,
            hasFestival = hasFestival,
            isMonthFillMode = isMonthFillMode,
            onClick = onClick,
        )
        schedulePreview.forEach { content ->
            DayTag(content)
        }
        if (isMonthFillMode) {
            festivalPreview.forEach { content ->
                DayTag(content)
            }
        }
    }
}

/** 日格：公历日 + 农历/节日小字，选中态由背景与描边共同表达。 */
@Composable
private fun DayCircle(
    date: CalendarDate,
    dayLabel: String,
    isSelected: Boolean,
    hasFestival: Boolean,
    isMonthFillMode: Boolean,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .padding(1.dp)
            .aspectRatio(1f)
            .clip(CircleShape)
            .clickable {
                if (date.currMonth) {
                    onClick()
                }
            }
            .then(selectionDecoration(date.currMonth, isSelected, hasFestival))
    ) {
        Text(
            text = date.day.toString(),
            modifier = Modifier
                .fillMaxWidth()
                .clipToBounds(),
            color = dayNumberColor(colors, date.currMonth, isSelected, hasFestival),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Text(
            text = "  $dayLabel  ",
            modifier = Modifier
                .fillMaxWidth()
                .clipToBounds(),
            color = lunarLabelColor(colors, date.currMonth, isSelected, hasFestival, isMonthFillMode),
            fontSize = 10.sp,
            textAlign = TextAlign.Center,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
        )
    }
}

/** 选中态的装饰：节日格只描边，普通格描边 + 底色。 */
@Composable
private fun selectionDecoration(
    isCurrMonth: Boolean,
    isSelected: Boolean,
    hasFestival: Boolean,
): Modifier {
    if (!isCurrMonth || !isSelected) return Modifier
    val colors = MaterialTheme.colorScheme
    return if (hasFestival) {
        Modifier.border(1.dp, colors.onSecondaryContainer, CircleShape)
    } else {
        Modifier
            .background(colors.onSecondaryContainer)
            .border(1.dp, colors.onSecondaryContainer, CircleShape)
    }
}

/** 格下方的小标签：日程与节日共用同一套样式。 */
@Composable
private fun DayTag(content: String) {
    Text(
        text = content,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 3.dp, vertical = 1.dp)
            .background(
                MaterialTheme.colorScheme.surfaceContainerHigh,
                RoundedCornerShape(3.dp)
            ),
        color = MaterialTheme.colorScheme.onPrimaryContainer,
        fontSize = 10.sp,
        textAlign = TextAlign.Center,
        lineHeight = 1.4.em,
        overflow = TextOverflow.Ellipsis,
        maxLines = 1,
    )
}
