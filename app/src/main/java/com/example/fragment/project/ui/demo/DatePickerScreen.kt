package com.example.fragment.project.ui.demo

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch

@SuppressLint("UnrememberedMutableState")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerScreen() {
    val snackState = remember { SnackbarHostState() }
    val snackScope = rememberCoroutineScope()
    SnackbarHost(hostState = snackState, Modifier)
    val openDialog = remember { mutableStateOf(false) }
    val dateType = remember { mutableIntStateOf(0) }
    if (openDialog.value) {
        val datePickerState = rememberDatePickerState()
        val dateRangePickerState = rememberDateRangePickerState()
        val confirmEnabled = derivedStateOf {
            if (dateType.intValue == 0) {
                datePickerState.selectedDateMillis != null
            } else {
                dateRangePickerState.selectedStartDateMillis != null && dateRangePickerState.selectedEndDateMillis != null
            }
        }
        DatePickerDialog(
            onDismissRequest = {
                openDialog.value = false
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        openDialog.value = false
                        snackScope.launch {
                            snackState.showSnackbar(
                                if (dateType.intValue == 0) {
                                    "Selected date timestamp: ${datePickerState.selectedDateMillis}"
                                } else {
                                    "Saved range (timestamps): " +
                                            "${dateRangePickerState.selectedStartDateMillis!!..dateRangePickerState.selectedEndDateMillis!!}"
                                }
                            )
                        }
                    },
                    enabled = confirmEnabled.value
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        openDialog.value = false
                    }
                ) {
                    Text("Cancel")
                }
            }
        ) {
            if (dateType.intValue == 0) {
                DatePicker(state = datePickerState)
            } else {
                DateRangePicker(state = dateRangePickerState, modifier = Modifier.weight(1f))
            }
        }
    }
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AssistChip(
            onClick = {
                openDialog.value = true
                dateType.intValue = 0
            },
            label = { Text("选择日期") },
            leadingIcon = {
                Icon(
                    Icons.Filled.DateRange,
                    contentDescription = null,
                    Modifier.size(AssistChipDefaults.IconSize)
                )
            }
        )
        AssistChip(
            onClick = {
                openDialog.value = true
                dateType.intValue = 1
            },
            label = { Text("选择日期范围") },
            leadingIcon = {
                Icon(
                    Icons.Filled.DateRange,
                    contentDescription = null,
                    Modifier.size(AssistChipDefaults.IconSize)
                )
            }
        )
    }
}