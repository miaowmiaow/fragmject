package com.example.fragment.project.ui.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import com.example.fragment.project.R
import com.example.fragment.project.components.Calendar

@Composable
fun CalendarScreen() {
    val customCalendar = mutableMapOf(
        "20240418" to listOf("喝水", "吃饭", "睡觉"),
        "20240419" to listOf(
            "喝水",
            "吃饭",
            "睡觉",
            "喝水",
            "吃饭",
            "睡觉",
            "喝水",
            "吃饭",
            "睡觉"
        ),
    )
    Calendar(
        padding = 45.dp,
        hasCustomCalendar = {
            customCalendar.containsKey(it.getDay())
        }
    ) { date ->
        customCalendar[date.getDay()]?.let {
            itemsIndexed(items = it) { _, item ->
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
}