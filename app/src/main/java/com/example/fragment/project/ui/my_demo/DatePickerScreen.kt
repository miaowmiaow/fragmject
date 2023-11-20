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
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerScreen() {
    val scope = rememberCoroutineScope()
    val bottomSheetState = rememberStandardBottomSheetState(skipHiddenState = false)
    val scaffoldState = rememberBottomSheetScaffoldState(bottomSheetState)
    BottomSheetScaffold(
        sheetContent = {
            Row(
                Modifier
                    .background(colorResource(R.color.white))
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { scope.launch { bottomSheetState.hide() } },
                    modifier = Modifier
                        .width(50.dp)
                        .height(25.dp),
                    shape = RoundedCornerShape(5.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorResource(R.color.gray),
                        contentColor = colorResource(R.color.text_666)
                    ),
                    elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp),
                    border = BorderStroke(1.dp, colorResource(R.color.gray)),
                    contentPadding = PaddingValues(5.dp, 3.dp, 5.dp, 3.dp)
                ) {
                    Text(text = "取消", fontSize = 13.sp)
                }
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = { scope.launch { bottomSheetState.hide() } },
                    modifier = Modifier
                        .width(50.dp)
                        .height(25.dp),
                    shape = RoundedCornerShape(5.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorResource(R.color.theme_orange),
                        contentColor = colorResource(R.color.text_fff)
                    ),
                    elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp),
                    border = BorderStroke(1.dp, colorResource(R.color.gray)),
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
                modifier = Modifier
                    .background(colorResource(R.color.white))
                    .fillMaxWidth()
                    .padding(16.dp),
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
        },
        scaffoldState = scaffoldState,
        sheetPeekHeight = 0.dp,
        sheetDragHandle = null
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = {
                    scope.launch { bottomSheetState.expand() }
                },
                modifier = Modifier.height(30.dp),
                shape = RoundedCornerShape(3.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorResource(R.color.white),
                    contentColor = colorResource(R.color.theme_orange)
                ),
                elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp),
                border = BorderStroke(1.dp, colorResource(R.color.theme_orange)),
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