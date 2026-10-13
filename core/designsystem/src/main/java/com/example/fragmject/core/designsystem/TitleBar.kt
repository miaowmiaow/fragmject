package com.example.fragmject.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TitleBar(
    title: String,
    modifier: Modifier = Modifier,
    onNavigateUp: (() -> Unit)? = null,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
) {
    CenterAlignedTopAppBar(
        title = {
            Box(
                modifier = Modifier.height(45.dp)
            ) {
                Text(
                    text = title,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .basicMarquee(),
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        modifier = modifier
            .background(MaterialTheme.colorScheme.primaryContainer)
            .statusBarsPadding()
            .height(45.dp),
        navigationIcon = {
            if (onNavigateUp != null) {
                BackIcon(onNavigateUp)
            } else {
                navigationIcon()
            }
        },
        actions = actions,
        colors = topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    )
}

/**
 * 返回箭头：左上角 [IconButton] + ArrowBack，点击回调 [onNavigateUp]。
 * 供「返回上一页」的页面复用（TitleBar 与 AccountForm 等自定义顶栏共用）。
 */
@Composable
fun BackIcon(onNavigateUp: () -> Unit) {
    IconButton(
        modifier = Modifier.height(45.dp),
        onClick = onNavigateUp,
    ) {
        Icon(
            Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "返回",
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}
