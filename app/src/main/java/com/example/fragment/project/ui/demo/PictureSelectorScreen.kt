package com.example.fragment.project.ui.demo

import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.miaow.picture.selector.bean.MediaBean
import com.example.miaow.picture.selector.dialog.PictureSelectorCallback
import com.example.miaow.picture.selector.dialog.PictureSelectorDialog

@Composable
fun PictureSelectorScreen() {
    val context = LocalContext.current
    val activity = context as AppCompatActivity
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AssistChip(
            onClick = {
                PictureSelectorDialog
                    .newInstance()
                    .setPictureSelectorCallback(object : PictureSelectorCallback {
                        override fun onSelectedData(data: List<MediaBean>) {
                        }
                    })
                    .show(activity.supportFragmentManager)
            },
            label = { Text("打开相册") },
            leadingIcon = {
                Icon(
                    Icons.Filled.Home,
                    contentDescription = null,
                    Modifier.size(AssistChipDefaults.IconSize)
                )
            }
        )
    }
}