package com.example.fragment.project.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.ScrollableTabRow
import androidx.compose.material.Tab
import androidx.compose.material.TabRowDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import com.example.fragment.library.base.R
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.PagerState
import com.google.accompanist.pager.pagerTabIndicatorOffset

@OptIn(ExperimentalPagerApi::class)
@Composable
fun <T> TabBar(
    pagerState: PagerState,
    data: List<T>?,
    textMapping: (T) -> String,
    onClick: (index: Int) -> Unit
) {
    ScrollableTabRow(
        selectedTabIndex = pagerState.currentPage,
        modifier = Modifier
            .fillMaxWidth()
            .height(45.dp),
        backgroundColor = colorResource(R.color.white),
        edgePadding = 0.dp,
        indicator = { tabPositions ->
            TabRowDefaults.Indicator(
                modifier = Modifier.pagerTabIndicatorOffset(pagerState, tabPositions),
                color = colorResource(R.color.theme)
            )
        },
        divider = {
            TabRowDefaults.Divider(color = colorResource(R.color.transparent))
        }
    ) {
        data?.forEachIndexed { index, item ->
            Tab(
                text = { Text(textMapping(item)) },
                onClick = {
                    onClick(index)
                },
                selected = pagerState.currentPage == index,
                selectedContentColor = colorResource(R.color.theme),
                unselectedContentColor = colorResource(R.color.text_999)
            )
        }
    }
    TabRowDefaults.Divider(color = colorResource(R.color.line))
}