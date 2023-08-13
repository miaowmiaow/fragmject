package com.example.fragment.project.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fragment.project.R

@Composable
fun TitleBar(
    title: String,
    onClick: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(45.dp)
            .background(colorResource(R.color.theme))
    ) {
        IconButton(
            modifier = Modifier.height(45.dp),
            onClick = {
                onClick()
            }
        ) {
            Icon(
                Icons.Filled.ArrowBack,
                contentDescription = null,
                tint = colorResource(R.color.white)
            )
        }
        Text(
            text = title,
            fontSize = 16.sp,
            color = colorResource(R.color.text_fff),
            modifier = Modifier.align(Alignment.Center)
        )
    }
}
