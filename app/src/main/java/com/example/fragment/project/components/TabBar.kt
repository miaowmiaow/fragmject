package com.example.fragment.project.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun <T> TabBar(
    data: List<T>?,
    dataMapping: (T) -> String,
    pagerState: PagerState,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    selectedContentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    unselectedContentColor: Color = MaterialTheme.colorScheme.onSecondary,
    indicatorColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    onClick: (index: Int) -> Unit
) {
    Box(modifier = modifier) {
        if (!data.isNullOrEmpty()) {
            ScrollableTabRow(
                selectedTabIndex = pagerState.currentPage,
                modifier = modifier,
                containerColor = backgroundColor,
                edgePadding = 0.dp,
                indicator = { tabPositions ->
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .tabIndicatorOffset(tabPositions[pagerState.currentPage]),
                        contentAlignment = Alignment.Center
                    ) {
                        Spacer(
                            Modifier
                                .width(20.dp)
                                .height(3.dp)
                                .background(indicatorColor)
                        )
                    }
                },
            ) {
                data.forEachIndexed { index, item ->
                    Tab(
                        text = { Text(dataMapping(item)) },
                        onClick = { onClick(index) },
                        selected = pagerState.currentPage == index,
                        selectedContentColor = selectedContentColor,
                        unselectedContentColor = unselectedContentColor
                    )
                }
            }
        }
    }
}