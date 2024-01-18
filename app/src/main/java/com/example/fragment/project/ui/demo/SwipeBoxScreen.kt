package com.example.fragment.project.ui.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fragment.project.R
import com.example.fragment.project.components.SwipeBox

@Composable
fun SwipeBoxScreen() {
    var checked by remember { mutableStateOf(false) }
    SwipeBox(
        checked = checked,
        onCheckedChange = { checked = it },
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        bottomContent = {
            Row {
                Box(
                    modifier = Modifier
                        .width(70.dp)
                        .fillMaxHeight()
                        .background(colorResource(R.color.blue))
                ) {
                    Text(
                        text = "标为未读",
                        modifier = Modifier.align(Alignment.Center),
                        style = TextStyle.Default.copy(
                            color = colorResource(R.color.white),
                            fontSize = 12.sp
                        )
                    )
                }
                Box(
                    modifier = Modifier
                        .width(70.dp)
                        .fillMaxHeight()
                        .background(colorResource(R.color.yellow))
                ) {
                    Text(
                        text = "不显示",
                        modifier = Modifier.align(Alignment.Center),
                        style = TextStyle.Default.copy(
                            color = colorResource(R.color.white),
                            fontSize = 12.sp
                        )
                    )
                }
                Box(
                    modifier = Modifier
                        .width(70.dp)
                        .fillMaxHeight()
                        .background(colorResource(R.color.red))
                ) {
                    Text(
                        text = "删除",
                        modifier = Modifier.align(Alignment.Center),
                        style = TextStyle.Default.copy(
                            color = colorResource(R.color.white),
                            fontSize = 12.sp
                        )
                    )
                }
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(colorResource(R.color.white))
                .padding(20.dp, 10.dp)
        ) {
            Text(
                text = "小美",
                color = colorResource(R.color.text_333),
                fontSize = 14.sp
            )
            Spacer(Modifier.size(5.dp))
            Text(
                text = "        一个人在家好无聊呀",
                color = colorResource(R.color.text_666),
                fontSize = 12.sp
            )
        }
    }
}