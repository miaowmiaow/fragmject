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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.fragment.project.WanTheme

@Composable
internal fun DayContent(
    date: CalendarDate,
    isMonthFillMode: Boolean,
    onClick: () -> Unit = {}
) {
    val schedule by date.schedule.collectAsStateWithLifecycle()
    val selectedDay by date.selectedDay.collectAsStateWithLifecycle()
    Column {
        Column(modifier = Modifier
            .padding(1.dp)
            .aspectRatio(1f)
            .clip(CircleShape)
            .clickable {
                if (date.currMonth) {
                    onClick()
                }
            }
            .then(
                if (date.currMonth && selectedDay) {
                    Modifier
                        .background(
                            if (date.isFestival()) {
                                Color.Transparent
                            } else {
                                MaterialTheme.colorScheme.onSecondaryContainer
                            }
                        )
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.onSecondaryContainer,
                            CircleShape
                        )
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
                color = if (date.currMonth) {
                    if (selectedDay && !date.isFestival()) {
                        MaterialTheme.colorScheme.secondaryContainer
                    } else {
                        MaterialTheme.colorScheme.onPrimary
                    }
                } else {
                    MaterialTheme.colorScheme.onTertiary
                },
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
                color = if (date.currMonth && selectedDay) {
                    if (date.isFestival()) {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    } else {
                        MaterialTheme.colorScheme.secondaryContainer
                    }
                } else if (date.currMonth && date.isFestival() && !isMonthFillMode) {
                    MaterialTheme.colorScheme.onSecondaryContainer
                } else if (!date.currMonth && date.isFestival() && !isMonthFillMode) {
                    WanTheme.alphaOrange
                } else {
                    MaterialTheme.colorScheme.onTertiary
                },
                fontSize = 10.sp,
                textAlign = TextAlign.Center,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
            )
        }
        schedule.forEach {
            Text(
                text = it,
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
                            MaterialTheme.colorScheme.surfaceContainerHigh,
                            RoundedCornerShape(3.dp)
                        ),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
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
