package com.example.fragment.project.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fragment.project.R

@Composable
fun DatePicker(
    onSelectYear: (item: String) -> Unit,
    onSelectMonth: (item: String) -> Unit,
    onSelectDay: (item: String) -> Unit,
) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(16.dp),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(175.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            WheelPicker(
                data = (1900..2051).toList().map { "$it" },
                selectIndex = 0,
                visibleCount = 5,
                modifier = Modifier
                    .width(70.dp)
                    .height(175.dp),
                onSelect = { _, item ->
                    onSelectYear(item)
                }
            ) {
                Text(
                    text = it,
                    fontSize = 14.sp
                )
            }
            Text(
                text = "/",
                fontSize = 15.sp
            )
            WheelPicker(
                data = (1..12).map { it.toString().padStart(2, '0') },
                selectIndex = 0,
                visibleCount = 5,
                modifier = Modifier
                    .width(70.dp)
                    .height(175.dp),
                onSelect = { _, item ->
                    onSelectMonth(item)
                }
            ) {
                Text(
                    text = it,
                    fontSize = 14.sp
                )
            }
            Text(
                text = "/",
                fontSize = 15.sp
            )
            WheelPicker(
                data = (1..31).map { it.toString().padStart(2, '0') },
                selectIndex = 0,
                visibleCount = 5,
                modifier = Modifier
                    .width(70.dp)
                    .height(175.dp),
                onSelect = { _, item ->
                    onSelectDay(item)
                }
            ) {
                Text(
                    text = it,
                    fontSize = 14.sp
                )
            }
        }
        Column(Modifier.height(175.dp)) {
            Spacer(
                Modifier
                    .background(colorResource(R.color.bb_white))
                    .fillMaxWidth()
                    .weight(1f)
            )
            Spacer(
                Modifier
                    .background(
                        colorResource(R.color.one_b_black),
                        RoundedCornerShape(5.dp)
                    )
                    .fillMaxWidth()
                    .height(30.dp)
            )
            Spacer(
                Modifier
                    .background(colorResource(R.color.bb_white))
                    .fillMaxWidth()
                    .weight(1f)
            )
        }
    }
}
