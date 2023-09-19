package com.example.fragment.project.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fragment.project.R

@Composable
fun ArrowRightItem(
    title: String,
    description: String = "",
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .background(colorResource(R.color.white))
            .fillMaxWidth()
            .height(45.dp)
            .clickable {
                onClick()
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            modifier = Modifier
                .padding(start = 25.dp),
            fontSize = 13.sp,
            color = colorResource(R.color.text_333),
        )
        Spacer(
            Modifier.width(5.dp)
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(17.dp)
                .padding(end = 25.dp),
            contentAlignment = Alignment.BottomStart
        ) {
            Text(
                text = description,
                fontSize = 10.sp,
                color = colorResource(R.color.text_999),
            )
        }
        Image(
            painter = painterResource(id = R.drawable.ic_right),
            contentDescription = "",
            modifier = Modifier.padding(start = 25.dp, end = 25.dp)
        )
    }
}
