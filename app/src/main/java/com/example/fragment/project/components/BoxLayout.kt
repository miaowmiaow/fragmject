package com.example.fragment.project.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import com.example.fragment.project.R

@Composable
fun BoxLayout(
    isLoading: Boolean,
    innerPadding: PaddingValues = PaddingValues(),
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
    ) {
        content()
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = colorResource(id = R.color.theme_orange)
            )
        }
    }
}