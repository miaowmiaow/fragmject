package com.example.fragment.project.ui.my_demo

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Text
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fragment.project.R
import com.example.fragment.project.components.DatePicker
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun DatePickerScreen() {
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
    )
    ModalBottomSheetLayout(
        sheetState = sheetState,
        sheetContent = {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { scope.launch { sheetState.hide() } },
                    modifier = Modifier
                        .width(50.dp)
                        .height(25.dp),
                    elevation = ButtonDefaults.elevation(0.dp, 0.dp, 0.dp),
                    shape = RoundedCornerShape(5.dp),
                    border = BorderStroke(1.dp, colorResource(R.color.gray)),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = colorResource(R.color.gray),
                        contentColor = colorResource(R.color.text_666)
                    ),
                    contentPadding = PaddingValues(5.dp, 3.dp, 5.dp, 3.dp)
                ) {
                    Text(text = "取消", fontSize = 13.sp)
                }
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = { scope.launch { sheetState.hide() } },
                    modifier = Modifier
                        .width(50.dp)
                        .height(25.dp),
                    elevation = ButtonDefaults.elevation(0.dp, 0.dp, 0.dp),
                    shape = RoundedCornerShape(5.dp),
                    border = BorderStroke(1.dp, colorResource(R.color.gray)),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = colorResource(R.color.theme_orange),
                        contentColor = colorResource(R.color.text_fff)
                    ),
                    contentPadding = PaddingValues(5.dp, 3.dp, 5.dp, 3.dp)
                ) {
                    Text(text = "确定", fontSize = 13.sp)
                }
            }
            Spacer(
                Modifier
                    .background(colorResource(R.color.line))
                    .fillMaxWidth()
                    .height(1.dp)
            )
            DatePicker(
                onSelectYear = {
                    println("year: $it")
                },
                onSelectMonth = {
                    println("month: $it")
                },
                onSelectDay = {
                    println("day: $it")
                }
            )
        }
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = {
                    scope.launch {
                        sheetState.show()
                    }
                },
                modifier = Modifier.height(30.dp),
                elevation = ButtonDefaults.elevation(0.dp, 0.dp, 0.dp),
                shape = RoundedCornerShape(3.dp),
                border = BorderStroke(1.dp, colorResource(R.color.theme_orange)),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = colorResource(R.color.white),
                    contentColor = colorResource(R.color.theme_orange)
                ),
                contentPadding = PaddingValues(3.dp, 2.dp, 3.dp, 2.dp)
            ) {
                Text(
                    text = "日期选择demo",
                    fontSize = 12.sp
                )
            }
        }
    }
}