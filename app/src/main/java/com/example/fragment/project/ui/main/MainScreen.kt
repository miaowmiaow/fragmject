package com.example.fragment.project.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fragment.library.base.compose.theme.WanTheme
import com.example.fragment.project.R
import com.example.fragment.project.bean.HotKeyBean
import com.example.fragment.project.bean.TreeBean
import com.example.fragment.project.components.LoopVerticalPager
import com.example.fragment.project.ui.main.home.HomeScreen
import com.example.fragment.project.ui.main.my.MyScreen
import com.example.fragment.project.ui.main.nav.NavScreen
import com.example.fragment.project.ui.main.project.ProjectScreen
import com.google.accompanist.pager.ExperimentalPagerApi

@Composable
fun MainScreen(
    hotKey: List<HotKeyBean>,
    tree: List<TreeBean>,
    onNavigateToLogin: () -> Unit = {},
    onNavigateToMyCoin: () -> Unit = {},
    onNavigateToMyCollect: () -> Unit = {},
    onNavigateToMyShare: () -> Unit = {},
    onNavigateToSearch: (key: String) -> Unit = {},
    onNavigateToShareArticle: () -> Unit = {},
    onNavigateToSetting: () -> Unit = {},
    onNavigateToSystem: (cid: String) -> Unit = {},
    onNavigateToUser: (userId: String) -> Unit = {},
    onNavigateToWeb: (url: String) -> Unit = {},
) {
    var navIndex by rememberSaveable { mutableStateOf(0) }
    val navItems = listOf(
        NavigationItem("首页", R.drawable.ic_bottom_bar_home),
        NavigationItem("导航", R.drawable.ic_bottom_bar_navigation),
        NavigationItem("项目", R.drawable.ic_bottom_bar_project),
        NavigationItem("我的", R.drawable.ic_bottom_bar_user),
    )
    Scaffold(
        modifier = Modifier.systemBarsPadding(),
        topBar = {
            SearchBar(
                data = hotKey,
                onNavigateToSearch = onNavigateToSearch,
                onNavigateToShareArticle = onNavigateToShareArticle
            )
        },
        bottomBar = {
            NavigationBar(
                items = navItems,
                onClick = { index ->
                    navIndex = index
                }
            )
        }
    ) { innerPadding ->
        val saveableStateHolder = rememberSaveableStateHolder()
        Box(Modifier.padding(innerPadding)) {
            when (navIndex) {
                0 -> saveableStateHolder.SaveableStateProvider(navItems[0].label) {
                    HomeScreen(
                        onNavigateToLogin = onNavigateToLogin,
                        onNavigateToSystem = onNavigateToSystem,
                        onNavigateToUser = onNavigateToUser,
                        onNavigateToWeb = onNavigateToWeb,
                    )
                }
                1 -> saveableStateHolder.SaveableStateProvider(navItems[1].label) {
                    NavScreen(
                        tree = tree,
                        onNavigateToSystem = onNavigateToSystem,
                        onNavigateToWeb = onNavigateToWeb,
                    )
                }
                2 -> saveableStateHolder.SaveableStateProvider(navItems[2].label) {
                    ProjectScreen(
                        onNavigateToLogin = onNavigateToLogin,
                        onNavigateToSystem = onNavigateToSystem,
                        onNavigateToUser = onNavigateToUser,
                        onNavigateToWeb = onNavigateToWeb
                    )
                }
                3 -> saveableStateHolder.SaveableStateProvider(navItems[3].label) {
                    MyScreen(
                        onNavigateToLogin = onNavigateToLogin,
                        onNavigateToMyCoin = onNavigateToMyCoin,
                        onNavigateToMyCollect = onNavigateToMyCollect,
                        onNavigateToMyShare = onNavigateToMyShare,
                        onNavigateToSetting = onNavigateToSetting,
                        onNavigateToUser = onNavigateToUser,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalPagerApi::class)
@Composable
fun SearchBar(
    data: List<HotKeyBean>?,
    onNavigateToSearch: (key: String) -> Unit = {},
    onNavigateToShareArticle: () -> Unit = {},
) {
    Row(
        modifier = Modifier
            .background(colorResource(R.color.theme))
            .fillMaxWidth()
            .height(45.dp)
            .padding(15.dp, 8.dp, 15.dp, 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .background(colorResource(R.color.three_nine_gray), RoundedCornerShape(50))
                .weight(1f)
                .fillMaxHeight(),
            contentAlignment = Alignment.CenterStart
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                modifier = Modifier.padding(10.dp, 5.dp, 0.dp, 5.dp),
                tint = colorResource(R.color.white)
            )
            LoopVerticalPager(
                data = data
            ) { _, _, item ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable {
                            onNavigateToSearch(item.name)
                        },
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = item.name,
                        modifier = Modifier.padding(start = 40.dp),
                        fontSize = 13.sp,
                        color = colorResource(R.color.text_fff)
                    )
                }
            }
        }
        Spacer(Modifier.width(15.dp))
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = null,
            modifier = Modifier
                .padding(0.dp, 5.dp, 0.dp, 5.dp)
                .clickable {
                    onNavigateToShareArticle()
                },
            tint = colorResource(R.color.white)
        )
    }
}

@Composable
fun NavigationBar(
    items: List<NavigationItem> = listOf(),
    onClick: (index: Int) -> Unit
) {
    var currItem by rememberSaveable { mutableStateOf(0) }

    BottomNavigation(
        modifier = Modifier,
        backgroundColor = colorResource(R.color.white)
    ) {
        items.forEachIndexed { index, item ->
            BottomNavigationItem(
                selected = currItem == index,
                onClick = {
                    currItem = index
                    onClick(index)
                },
                icon = {
                    Icon(
                        painter = painterResource(id = item.resId),
                        contentDescription = null,
                        modifier = Modifier
                            .size(25.dp)
                            .padding(bottom = 3.dp),
                        tint = colorResource(
                            if (currItem == index)
                                item.selectedColor
                            else
                                item.unselectedColor
                        )
                    )
                },
                label = {
                    Text(
                        text = item.label,
                        fontSize = 13.sp,
                    )
                },
                selectedContentColor = colorResource(item.selectedColor),
                unselectedContentColor = colorResource(item.unselectedColor)
            )
        }
    }
}

data class NavigationItem(
    val label: String,
    val resId: Int,
    val selectedColor: Int = R.color.theme_orange,
    val unselectedColor: Int = R.color.theme
)

@Preview(showBackground = true, backgroundColor = 0xFFF0EAE2)
@Composable
fun SearchBarPreview() {
    WanTheme { SearchBar(data = listOf(HotKeyBean(name = "问答"))) }
}

@Preview(showBackground = true, backgroundColor = 0xFFF0EAE2)
@Composable
fun WanBottomNavigationPreview() {
    val navItems = listOf(
        NavigationItem("首页", R.drawable.ic_bottom_bar_home),
        NavigationItem("导航", R.drawable.ic_bottom_bar_navigation),
        NavigationItem("项目", R.drawable.ic_bottom_bar_project),
        NavigationItem("我的", R.drawable.ic_bottom_bar_user),
    )
    WanTheme { NavigationBar(items = navItems, onClick = { _ -> }) }
}