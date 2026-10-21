package com.example.fragmject.feature.demo.ui.calendar

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.fragmject.feature.demo.impl.R

@Composable
internal fun ScheduleContent(
    spec: ScheduleContentSpec,
    listState: LazyListState,
    userScrollEnabled: () -> Boolean,
    offsetProvider: () -> Int,
    onRemoveSchedule: (String) -> Unit,
) {
    val date = spec.date ?: return
    val festivals = remember(date) { date.getFestival() }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .requiredHeight(spec.height - LunarHeight - TipArrowHeight)
            .offset { IntOffset(x = 0, y = offsetProvider()) }
            .background(MaterialTheme.colorScheme.background)
    ) {
        ScheduleTipArrow(spec.mode)
        LunarInfoText(date)
        if (spec.schedule.isEmpty() && festivals.isEmpty()) {
            EmptyScheduleHint(spec, offsetProvider)
        } else {
            ScheduleList(spec, date, festivals, listState, userScrollEnabled, onRemoveSchedule)
        }
    }
}

/** 折叠态/展开态之间衔接的三角指示。 */
@Composable
private fun ScheduleTipArrow(mode: CalendarMode) {
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
}

/** 农历与生肖文案。 */
@Composable
private fun LunarInfoText(date: CalendarDate) {
    Text(
        text = date.lunarYear() + " " + date.animalsYear() + " " + date.lunarMonth() + date.lunarDay(),
        modifier = Modifier
            .height(LunarHeight)
            .padding(10.dp)
    )
}

/** 当天既无日程也无节日时的占位提示。 */
@Composable
private fun EmptyScheduleHint(
    spec: ScheduleContentSpec,
    offsetProvider: () -> Int,
) {
    val density = LocalDensity.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .requiredHeight(
                spec.height - TipArrowHeight - LunarHeight - with(density) { offsetProvider().toDp() }
            ),
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
            color = MaterialTheme.colorScheme.onTertiaryContainer
        )
    }
}

/** 日程与节日列表；日程项可点击删除，节日项仅展示。 */
@Composable
private fun ScheduleList(
    spec: ScheduleContentSpec,
    date: CalendarDate,
    festivals: List<String>,
    listState: LazyListState,
    userScrollEnabled: () -> Boolean,
    onRemoveSchedule: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        userScrollEnabled = userScrollEnabled()
    ) {
        itemsIndexed(
            items = spec.schedule,
            key = { _, item -> item }
        ) { _, item ->
            ScheduleRow(text = item, date = date, onClick = { onRemoveSchedule(item) })
        }
        itemsIndexed(
            items = festivals,
            key = { _, item -> item }
        ) { _, item ->
            ScheduleRow(text = item, date = date)
        }
    }
}

/** 单条日程/节日卡片；[onClick] 为空时不可点击。 */
@Composable
private fun ScheduleRow(
    text: String,
    date: CalendarDate,
    onClick: (() -> Unit)? = null,
) {
    val clickModifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clipToBounds()
            .then(clickModifier)
            .background(MaterialTheme.colorScheme.surface)
            .fillMaxWidth()
            .padding(10.dp)
    ) {
        Text(text = text)
        Text(text = "${date.year}年${date.month}月${date.day}日")
    }
}
