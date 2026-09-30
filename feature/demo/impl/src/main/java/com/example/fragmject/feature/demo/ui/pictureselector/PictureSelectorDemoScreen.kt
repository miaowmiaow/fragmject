package com.example.fragmject.feature.demo.ui.pictureselector

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.fragmject.core.designsystem.AppTheme
import com.example.fragmject.core.navigation.contract.LocalPictureNavigator

@Composable
fun PictureSelectorDemoScreen() {
    val pictureNavigator = LocalPictureNavigator.current
    val selectedUris by pictureNavigator.selectedUris.collectAsStateWithLifecycle()
    val selectedUri = selectedUris.firstOrNull()
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AssistChip(
            onClick = { pictureNavigator.openPictureSelector() },
            label = { Text("打开相册") },
            leadingIcon = {
                Icon(
                    Icons.Filled.Home,
                    contentDescription = null,
                    Modifier.size(AssistChipDefaults.IconSize)
                )
            },
            colors = AssistChipDefaults.assistChipColors(
                labelColor = MaterialTheme.colorScheme.primaryContainer,
                leadingIconContentColor = MaterialTheme.colorScheme.primaryContainer
            ),
            border = AssistChipDefaults.assistChipBorder(
                true,
                borderColor = MaterialTheme.colorScheme.primaryContainer
            )
        )
        Spacer(modifier = Modifier.height(24.dp))
        if (selectedUri != null) {
            AsyncImage(
                model = selectedUri,
                contentDescription = "选中的图片",
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(200.dp)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .background(Color.Gray.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text("尚未选择图片", color = Color.Gray)
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF0F0F0)
@Composable
fun PictureSelectorDemoScreenPreview() {
    AppTheme { PictureSelectorDemoScreen() }
}