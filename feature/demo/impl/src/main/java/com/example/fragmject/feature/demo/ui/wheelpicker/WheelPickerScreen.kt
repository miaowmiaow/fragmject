package com.example.fragmject.feature.demo.ui.wheelpicker

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
import com.example.fragmject.core.designsystem.AppTheme
import com.example.fragmject.core.ui.components.WheelPicker
import kotlinx.coroutines.launch

/** 时/分可选值：两位补零，滚轮里只展示字符串。 */
private val HOURS = (0..23).map { it.toString().padStart(2, '0') }
private val MINUTES = (0..59).map { it.toString().padStart(2, '0') }

@Composable
fun WheelPickerScreen() {
    val hour = remember { mutableStateOf("") }
    val minute = remember { mutableStateOf("") }
    val snackState = remember { SnackbarHostState() }
    val snackScope = rememberCoroutineScope()
    SnackbarHost(hostState = snackState, Modifier)
    Column {
        Box(modifier = Modifier.weight(1f))
        WheelPickerActionBar(
            onCancel = { },
            onConfirm = {
                snackScope.launch {
                    snackState.showSnackbar(
                        "Selected date timestamp: ${hour.value} / ${minute.value}"
                    )
                }
            },
        )
        HorizontalDivider()
        TimeWheelPickerPanel(
            onHourSelect = { hour.value = it },
            onMinuteSelect = { minute.value = it },
        )
    }
}

/** 底部操作栏：取消 / 确定。 */
@Composable
private fun WheelPickerActionBar(
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
) {
    Row(
        Modifier
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        WheelPickerActionButton(
            label = "取消",
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            borderColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            onClick = onCancel,
        )
        Spacer(Modifier.weight(1f))
        WheelPickerActionButton(
            label = "确定",
            containerColor = MaterialTheme.colorScheme.onSurface,
            borderColor = MaterialTheme.colorScheme.onSurface,
            onClick = onConfirm,
        )
    }
}

/** 操作栏按钮：两枚按钮仅配色不同。 */
@Composable
private fun WheelPickerActionButton(
    label: String,
    containerColor: Color,
    borderColor: Color,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .width(50.dp)
            .height(25.dp),
        shape = RoundedCornerShape(5.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = MaterialTheme.colorScheme.surface
        ),
        elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp),
        border = BorderStroke(1.dp, borderColor),
        contentPadding = PaddingValues(5.dp, 3.dp, 5.dp, 3.dp)
    ) {
        Text(text = label, fontSize = 13.sp)
    }
}

/** 时/分双滚轮 + 上下白色遮罩。 */
@Composable
private fun TimeWheelPickerPanel(
    onHourSelect: (String) -> Unit,
    onMinuteSelect: (String) -> Unit,
) {
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
                data = HOURS,
                selectIndex = 0,
                visibleCount = 5,
                modifier = pickerModifier,
                onSelect = { _, item -> onHourSelect(item) }
            ) {
                Text(text = it, fontSize = 14.sp)
            }
            Text(text = "/", fontSize = 15.sp)
            WheelPicker(
                data = MINUTES,
                selectIndex = 0,
                visibleCount = 5,
                modifier = pickerModifier,
                onSelect = { _, item -> onMinuteSelect(item) }
            ) {
                Text(text = it, fontSize = 14.sp)
            }
        }
        WheelPickerMask()
    }
}

/** 上下渐隐遮罩：中间留出 30dp 的选中条。 */
@Composable
private fun WheelPickerMask() {
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

@Preview(showBackground = true, backgroundColor = 0xFFF0F0F0)
@Composable
fun WheelPickerScreenPreview() {
    AppTheme { WheelPickerScreen() }
}
