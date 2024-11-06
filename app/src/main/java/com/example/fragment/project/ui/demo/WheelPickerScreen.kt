package com.example.fragment.project.ui.demo

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fragment.project.WanTheme
import com.example.fragment.project.components.WheelPicker
import kotlinx.coroutines.launch

@Composable
fun WheelPickerScreen() {
    val hour = remember { mutableStateOf("") }
    val minute = remember { mutableStateOf("") }
    val snackState = remember { SnackbarHostState() }
    val snackScope = rememberCoroutineScope()
    SnackbarHost(hostState = snackState, Modifier)
    Column {
        Box(modifier = Modifier.weight(1f))
        Row(
            Modifier
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = { },
                modifier = Modifier
                    .width(50.dp)
                    .height(25.dp),
                shape = RoundedCornerShape(5.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ),
                elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primaryContainer),
                contentPadding = PaddingValues(5.dp, 3.dp, 5.dp, 3.dp)
            ) {
                Text(text = "取消", fontSize = 13.sp)
            }
            Spacer(Modifier.weight(1f))
            Button(
                onClick = {
                    snackScope.launch {
                        snackState.showSnackbar(
                            "Selected date timestamp: ${hour.value} / ${minute.value}"
                        )
                    }
                },
                modifier = Modifier
                    .width(50.dp)
                    .height(25.dp),
                shape = RoundedCornerShape(5.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = WanTheme.orange,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primaryContainer),
                contentPadding = PaddingValues(5.dp, 3.dp, 5.dp, 3.dp)
            ) {
                Text(text = "确定", fontSize = 13.sp)
            }
        }
        HorizontalDivider()
        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surfaceContainer)
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
                val pickerModifier = Modifier
                    .width(70.dp)
                    .height(175.dp)
                WheelPicker(
                    data = (0..23).map { it.toString().padStart(2, '0') },
                    selectIndex = 0,
                    visibleCount = 5,
                    modifier = pickerModifier,
                    onSelect = { _, item ->
                        hour.value = item
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
                    data = (0..59).map { it.toString().padStart(2, '0') },
                    selectIndex = 0,
                    visibleCount = 5,
                    modifier = pickerModifier,
                    onSelect = { _, item ->
                        minute.value = item
                    }
                ) {
                    Text(
                        text = it,
                        fontSize = 14.sp
                    )
                }
            }
            Column(Modifier.height(175.dp)) {
                val whiteMaskModifier = Modifier
                    .background(Color(0xBBFFFFFF))
                    .fillMaxWidth()
                    .weight(1f)
                Spacer(whiteMaskModifier)
                Spacer(
                    Modifier
                        .background(Color(0x1B000000), RoundedCornerShape(5.dp))
                        .fillMaxWidth()
                        .height(30.dp)
                )
                Spacer(whiteMaskModifier)
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF0F0F0)
@Composable
fun WheelPickerScreenPreview() {
    WanTheme { WheelPickerScreen() }
}