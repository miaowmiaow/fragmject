package com.example.fragment.project.ui.demo

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fragment.project.WanTheme
import com.example.fragment.project.components.SwipeBox

@Composable
fun SwipeBoxScreen() {
    val context = LocalContext.current
    SwipeBox(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        actionWidth = 70.dp,
        startAction = listOf {
            Box(
                modifier = Modifier
                    .background(WanTheme.green)
                    .fillMaxSize()
                    .clickable {
                        Toast
                            .makeText(context, "置顶", Toast.LENGTH_SHORT)
                            .show()
                    }
            ) {
                Text(
                    text = "置顶",
                    modifier = Modifier.align(Alignment.Center),
                    style = TextStyle.Default.copy(
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 12.sp
                    )
                )
            }
        },
        startFillAction = {
            Box(
                modifier = Modifier
                    .background(WanTheme.pink)
                    .fillMaxSize()
                    .clickable {
                        Toast
                            .makeText(context, "取消置顶", Toast.LENGTH_SHORT)
                            .show()
                    }
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .width(70.dp)
                        .fillMaxHeight()
                ) {
                    Text(
                        text = "取消置顶",
                        modifier = Modifier.align(Alignment.Center),
                        style = TextStyle.Default.copy(
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontSize = 12.sp
                        )
                    )
                }
            }
        },
        endAction = listOf(
            {
                Box(
                    modifier = Modifier
                        .background(WanTheme.blue)
                        .fillMaxSize()
                        .clickable {
                            Toast
                                .makeText(context, "标为未读", Toast.LENGTH_SHORT)
                                .show()
                        }
                ) {
                    Text(
                        text = "标为未读",
                        modifier = Modifier.align(Alignment.Center),
                        style = TextStyle.Default.copy(
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontSize = 12.sp
                        )
                    )
                }
            },
            {
                Box(
                    modifier = Modifier
                        .background(WanTheme.yellow)
                        .fillMaxSize()
                        .clickable {
                            Toast
                                .makeText(context, "不显示", Toast.LENGTH_SHORT)
                                .show()
                        }
                ) {
                    Text(
                        text = "不显示",
                        modifier = Modifier.align(Alignment.Center),
                        style = TextStyle.Default.copy(
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontSize = 12.sp
                        )
                    )
                }
            }
        ),
        endFillAction = {
            Box(
                modifier = Modifier
                    .background(WanTheme.red)
                    .fillMaxSize()
                    .clickable {
                        Toast
                            .makeText(context, "删除", Toast.LENGTH_SHORT)
                            .show()
                    }
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .width(70.dp)
                        .fillMaxHeight()
                ) {
                    Text(
                        text = "删除",
                        modifier = Modifier.align(Alignment.Center),
                        style = TextStyle.Default.copy(
                            color = MaterialTheme.colorScheme.onPrimary,
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
                .background(MaterialTheme.colorScheme.secondaryContainer)
                .padding(20.dp, 10.dp)
        ) {
            Text(
                text = "小美",
                fontSize = 14.sp
            )
            Spacer(Modifier.size(5.dp))
            Text(
                text = "我的电脑坏了，你能过来看看嘛。",
                fontSize = 12.sp
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF0F0F0)
@Composable
fun SwipeBoxScreenPreview() {
    WanTheme { SwipeBoxScreen() }
}