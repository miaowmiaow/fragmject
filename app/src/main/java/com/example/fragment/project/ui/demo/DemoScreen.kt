package com.example.fragment.project.ui.demo

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import com.example.fragment.project.R
import com.example.fragment.project.components.TitleBar
import kotlinx.coroutines.launch

@Composable
fun DemoScreen(
    onNavigateUp: () -> Unit = {}
) {
    val drawerState = rememberDrawerState(DrawerValue.Open)
    val scope = rememberCoroutineScope()
    val tabs = listOf(
        "日期",
        "滚轮",
        "菜单",
        "全文",
        "权限",
        "相册",
        "滚动",
        "拖动",
        "多点触控"
    )
    var selectedTab by remember { mutableStateOf(tabs[0]) }
    Scaffold(
        topBar = {
            TitleBar(
                title = "组件Demo",
                navigationIcon = {
                    IconButton(
                        onClick = {
                            scope.launch {
                                if (drawerState.isClosed) {
                                    drawerState.open()
                                } else {
                                    drawerState.close()
                                }
                            }
                        }
                    ) {
                        Icon(
                            Icons.Default.Menu,
                            contentDescription = null,
                            tint = colorResource(R.color.white)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = null,
                            tint = colorResource(R.color.white)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        ModalNavigationDrawer(
            drawerContent = {
                ModalDrawerSheet {
                    Spacer(Modifier.height(12.dp))
                    tabs.forEach { tab ->
                        NavigationDrawerItem(
                            label = { Text(tab) },
                            selected = tab == selectedTab,
                            onClick = {
                                scope.launch { drawerState.close() }
                                selectedTab = tab
                            },
                            modifier = Modifier
                                .width(200.dp)
                                .padding(NavigationDrawerItemDefaults.ItemPadding)
                        )
                    }
                }
            },
            drawerState = drawerState,
            modifier = Modifier.padding(innerPadding),
            content = {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when (tabs.indexOf(selectedTab)) {
                        0 -> DatePickerScreen()
                        1 -> WheelPickerScreen()
                        2 -> DropdownMenuScreen()
                        3 -> FullTextScreen()
                        4 -> PermissionScreen()
                        5 -> PictureSelectorScreen()
                        6 -> ScrollableScreen()
                        7 -> DraggableScreen()
                        8 -> TransformableScreen()
                    }
                }
            }
        )
    }
}

