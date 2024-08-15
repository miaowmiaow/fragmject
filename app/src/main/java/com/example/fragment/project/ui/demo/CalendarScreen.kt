package com.example.fragment.project.ui.demo

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
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.fragment.project.R
import com.example.fragment.project.WanTheme
import com.example.fragment.project.components.WhiteTextField
import com.example.fragment.project.components.calendar.Calendar
import com.example.fragment.project.components.calendar.rememberCalendarState

@Composable
fun CalendarScreen() {
    Box(contentAlignment = Alignment.BottomEnd) {
        val calendarState = rememberCalendarState()
        var openDialog by remember { mutableStateOf(false) }
        var text by rememberSaveable { mutableStateOf("") }
        if (openDialog) {
            Dialog(onDismissRequest = { openDialog = false }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardColors(
                        containerColor = colorResource(id = R.color.theme),
                        contentColor = colorResource(id = R.color.theme),
                        disabledContainerColor = colorResource(id = R.color.theme_orange),
                        disabledContentColor = colorResource(id = R.color.theme_orange),
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
                                lineHeight = 14.sp
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
                                onClick = { openDialog = false },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "取消",
                                    fontSize = 14.sp,
                                    color = colorResource(R.color.text_999)
                                )
                            }
                            TextButton(
                                onClick = {
                                    calendarState.onSchedule(text)
                                    openDialog = false
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "确定",
                                    fontSize = 14.sp,
                                    color = colorResource(R.color.text_999)
                                )
                            }
                        }
                    }
                }
            }
        }
        Calendar(
            state = calendarState,
            modifier = Modifier.padding(vertical = 15.dp),
            onSelectedDateChange = { _, _, _ ->

            }
        )
        Button(
            onClick = {
                openDialog = true
            },
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(
                containerColor = colorResource(R.color.theme_orange),
                contentColor = colorResource(R.color.white)
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
}

@Preview(showBackground = true, backgroundColor = 0xFFF0F0F0)
@Composable
fun CalendarScreenPreview() {
    WanTheme { CalendarScreen() }
}