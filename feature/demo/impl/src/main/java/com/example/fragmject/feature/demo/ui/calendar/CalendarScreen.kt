package com.example.fragmject.feature.demo.ui.calendar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.fragmject.core.designsystem.AppColors
import com.example.fragmject.core.designsystem.AppTheme
import com.example.fragmject.feature.demo.impl.R
import com.example.fragmject.core.ui.components.WhiteTextField

@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel = hiltViewModel(),
) {
    val scheduleMap by viewModel.scheduleMap.collectAsStateWithLifecycle()
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val calendarState = rememberCalendarState()
    var openDialog by remember { mutableStateOf(false) }
    Box(contentAlignment = Alignment.BottomEnd) {
        Calendar(
            state = calendarState,
            modifier = Modifier.padding(vertical = 15.dp),
            scheduleMap = scheduleMap,
            selectedDate = selectedDate,
            onSelectDate = viewModel::selectDate,
            onRemoveSchedule = viewModel::removeSchedule,
            onSelectedDateChange = { _, _, _ ->
                // no-op：日期选中事件占位回调
            }
        )
        AddScheduleButton(onClick = { openDialog = true })
    }
    if (openDialog) {
        AddScheduleDialog(
            onDismiss = { openDialog = false },
            onConfirm = { text ->
                selectedDate?.let { viewModel.addSchedule(it, text) }
                openDialog = false
            },
        )
    }
}

/** 新建日程弹窗：输入文本后回传，取消/确认都只负责关闭。 */
@Composable
private fun AddScheduleDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var text by rememberSaveable { mutableStateOf("") }
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardColors(
                containerColor = AppColors.theme,
                contentColor = AppColors.theme,
                disabledContainerColor = AppColors.orange,
                disabledContentColor = AppColors.orange,
            )
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                WhiteTextField(
                    value = text,
                    onValueChange = { text = it },
                    textStyle = TextStyle.Default.copy(
                        fontSize = 14.sp,
                        lineHeight = 1.4.em
                    ),
                    placeholder = { Text("创建日程") },
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .padding(16.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "取消",
                            fontSize = 14.sp,
                        )
                    }
                    TextButton(
                        onClick = { onConfirm(text) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "确定",
                            fontSize = 14.sp,
                        )
                    }
                }
            }
        }
    }
}

/** 右下角的新建日程悬浮按钮。 */
@Composable
private fun AddScheduleButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        colors = ButtonDefaults.buttonColors(
            containerColor = AppColors.orange,
            contentColor = Color.White
        ),
        elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp),
        contentPadding = PaddingValues(15.dp),
        modifier = Modifier
            .padding(15.dp)
            .size(55.dp)
    ) {
        Icon(
            painter = painterResource(R.mipmap.ic_add),
            contentDescription = null
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF0F0F0)
@Composable
fun CalendarScreenPreview() {
    AppTheme { CalendarScreen() }
}
