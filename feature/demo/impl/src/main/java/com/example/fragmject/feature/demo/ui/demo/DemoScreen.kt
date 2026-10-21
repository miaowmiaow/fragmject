package com.example.fragmject.feature.demo.ui.demo

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.fragmject.core.designsystem.AppTheme
import com.example.fragmject.core.navigation.runtime.LocalOnNavigateUp
import com.example.fragmject.core.ui.components.TitleBar
import com.example.fragmject.feature.demo.ui.barcodescanning.BarcodeScanningScreen
import com.example.fragmject.feature.demo.ui.calendar.CalendarScreen
import com.example.fragmject.feature.demo.ui.fulltext.FullTextScreen
import com.example.fragmject.feature.demo.ui.gridselect.GridSelectScreen
import com.example.fragmject.feature.demo.ui.nestedscroll.NestedScroll1Screen
import com.example.fragmject.feature.demo.ui.nestedscroll.NestedScroll2Screen
import com.example.fragmject.feature.demo.ui.pictureselector.PictureSelectorDemoScreen
import com.example.fragmject.feature.demo.ui.swipebox.SwipeBoxScreen
import com.example.fragmject.feature.demo.ui.wheelpicker.WheelPickerScreen
import kotlinx.coroutines.launch

/** Demo 页签：抽屉顺序即内容分支下标。 */
private val DEMO_TABS = listOf(
    "日历控件",
    "滚轮控件",
    "全文控件",
    "侧滑控件",
    "调用相册",
    "网格选择",
    "扫码示例",
    "嵌套滚动1",
    "嵌套滚动2",
)

@Composable
fun DemoScreen() {
    val onNavigateUp = LocalOnNavigateUp.current
    val drawerState = rememberDrawerState(DrawerValue.Open)
    val scope = rememberCoroutineScope()
    var selectedTab by rememberSaveable { mutableStateOf(DEMO_TABS.first()) }
    Scaffold(
        topBar = {
            DemoTopBar(
                title = selectedTab,
                onToggleDrawer = {
                    scope.launch {
                        if (drawerState.isClosed) {
                            drawerState.open()
                        } else {
                            drawerState.close()
                        }
                    }
                },
                onClose = onNavigateUp,
            )
        },
    ) { innerPadding ->
        ModalNavigationDrawer(
            drawerContent = {
                DemoDrawerContent(
                    tabs = DEMO_TABS,
                    selectedTab = selectedTab,
                    onSelect = { tab ->
                        scope.launch { drawerState.close() }
                        selectedTab = tab
                    },
                )
            },
            drawerState = drawerState,
            modifier = Modifier.padding(innerPadding),
            content = { DemoContent(selectedTab = selectedTab) },
        )
    }
}

/** 顶栏：左侧抽屉开关，右侧关闭示例页。 */
@Composable
private fun DemoTopBar(
    title: String,
    onToggleDrawer: () -> Unit,
    onClose: () -> Unit,
) {
    TitleBar(
        title = title,
        navigationIcon = {
            IconButton(onClick = onToggleDrawer) {
                Icon(
                    Icons.Default.Menu,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        },
        actions = {
            IconButton(onClick = onClose) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    )
}

/** 抽屉里的示例列表。 */
@Composable
private fun DemoDrawerContent(
    tabs: List<String>,
    selectedTab: String,
    onSelect: (String) -> Unit,
) {
    ModalDrawerSheet(
        modifier = Modifier
            .fillMaxHeight()
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(Modifier.height(12.dp))
        tabs.forEach { tab ->
            NavigationDrawerItem(
                label = { Text(tab) },
                selected = tab == selectedTab,
                onClick = { onSelect(tab) },
                modifier = Modifier
                    .width(200.dp)
                    .padding(NavigationDrawerItemDefaults.ItemPadding)
            )
        }
    }
}

/** 当前页签对应的示例内容。 */
@Composable
private fun DemoContent(selectedTab: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when (DEMO_TABS.indexOf(selectedTab)) {
            0 -> CalendarScreen()
            1 -> WheelPickerScreen()
            2 -> FullTextScreen()
            3 -> SwipeBoxScreen()
            4 -> PictureSelectorDemoScreen()
            5 -> GridSelectScreen()
            6 -> BarcodeScanningScreen()
            7 -> NestedScroll1Screen()
            8 -> NestedScroll2Screen()
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF0F0F0)
@Composable
fun DemoScreenPreview() {
    AppTheme { DemoScreen() }
}
