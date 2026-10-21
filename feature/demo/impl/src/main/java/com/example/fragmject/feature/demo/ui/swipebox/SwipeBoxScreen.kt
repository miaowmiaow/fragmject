package com.example.fragmject.feature.demo.ui.swipebox

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fragmject.core.designsystem.AppColors
import com.example.fragmject.core.designsystem.AppTheme
import com.example.fragmject.core.ui.components.SwipeBox
import com.example.fragmject.core.ui.components.rememberSwipeBoxControl

@Composable
fun SwipeBoxScreen() {
    val context = LocalContext.current
    val control = rememberSwipeBoxControl()
    val showToast: (String) -> Unit = { text ->
        control.center()
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
    }
    SwipeBox(
        control = control,
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        actionWidth = 70.dp,
        startAction = listOf {
            SwipeActionLabel(color = AppColors.green, label = "置顶") { showToast("置顶") }
        },
        startFillAction = {
            SwipeFillActionLabel(
                color = AppColors.pink,
                label = "取消置顶",
                alignment = Alignment.CenterEnd,
            ) { showToast("取消置顶") }
        },
        endAction = listOf(
            { SwipeActionLabel(color = AppColors.blue, label = "标为未读") { showToast("标为未读") } },
            { SwipeActionLabel(color = AppColors.yellow, label = "不显示") { showToast("不显示") } },
        ),
        endFillAction = {
            SwipeFillActionLabel(
                color = AppColors.red,
                label = "删除",
                alignment = Alignment.CenterStart,
            ) { showToast("删除") }
        }
    ) {
        SwipeBoxDemoContent()
    }
}

/** 普通滑动动作：整格填色 + 居中文字。 */
@Composable
private fun SwipeActionLabel(
    color: Color,
    label: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .background(color)
            .fillMaxSize()
            .clickable(onClick = onClick)
    ) {
        ActionLabel(text = label, modifier = Modifier.align(Alignment.Center))
    }
}

/** 填充式滑动动作：整条填色，文字贴到与普通动作同宽的那一格。 */
@Composable
private fun SwipeFillActionLabel(
    color: Color,
    label: String,
    alignment: Alignment,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .background(color)
            .fillMaxSize()
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .align(alignment)
                .width(70.dp)
                .fillMaxHeight()
        ) {
            ActionLabel(text = label, modifier = Modifier.align(Alignment.Center))
        }
    }
}

/** 动作文字统一样式。 */
@Composable
private fun ActionLabel(text: String, modifier: Modifier) {
    Text(
        text = text,
        modifier = modifier,
        style = TextStyle.Default.copy(
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            fontSize = 12.sp
        )
    )
}

/** 被滑动的示例内容。 */
@Composable
private fun SwipeBoxDemoContent() {
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

@Preview(showBackground = true, backgroundColor = 0xFFF0F0F0)
@Composable
fun SwipeBoxScreenPreview() {
    AppTheme { SwipeBoxScreen() }
}
