package com.example.fragment.project.components.calendar

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.fragment.project.R
import kotlinx.coroutines.launch

@Composable
internal fun ScheduleContent(
    date: CalendarDate?,
    mode: CalendarMode,
    height: Dp,
    listState: LazyListState,
    userScrollEnabled: Boolean,
    offsetProvider: () -> Int,
) {
    if (date == null) return
    val scope = rememberCoroutineScope()
    val schedule by date.schedule.collectAsStateWithLifecycle()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .requiredHeight(height - LunarHeight - TipArrowHeight)
            .offset { IntOffset(x = 0, y = offsetProvider()) }
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
                    id = when (mode) {
                        CalendarMode.Week -> R.mipmap.ic_obtuse_bottom
                        CalendarMode.MonthFill -> R.mipmap.ic_obtuse_top
                        else -> R.mipmap.ic_line
                    }
                ),
                contentDescription = "",
                modifier = Modifier
                    .height(TipArrowHeight)
                    .aspectRatio(1f)
            )
        }
        Text(
            text = date.lunarYear() + " " + date.animalsYear() + " " + date.lunarMonth() + date.lunarDay(),
            modifier = Modifier
                .height(LunarHeight)
                .padding(10.dp)
        )
        if (schedule.isEmpty() && date.getFestival().isEmpty()) {
            val density = LocalDensity.current
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .requiredHeight(height - TipArrowHeight - LunarHeight - with(density) { offsetProvider().toDp() }),
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
                userScrollEnabled = userScrollEnabled
            ) {
                itemsIndexed(
                    items = schedule,
                ) { _, item ->
                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clipToBounds()
                            .clickable {
                                scope.launch {
                                    date.removeSchedule(item)
                                }
                            }
                            .background(colorResource(R.color.white))
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
                            .clip(RoundedCornerShape(10.dp))
                            .clipToBounds()
                            .background(colorResource(R.color.white))
                            .fillMaxWidth()
                            .padding(10.dp)
                    ) {
                        Text(text = item)
                        Text(text = "${date.year}年${date.month}月${date.day}日")
                    }
                }
            }
        }
    }
}