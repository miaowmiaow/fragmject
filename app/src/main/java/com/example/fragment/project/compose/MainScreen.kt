package com.example.fragment.project.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.bundleOf
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fragment.library.base.compose.LoopVerticalPager
import com.example.fragment.library.base.compose.theme.WanTheme
import com.example.fragment.library.common.activity.RouterActivity
import com.example.fragment.library.common.bean.HotKeyBean
import com.example.fragment.library.common.constant.Keys
import com.example.fragment.library.common.constant.Router
import com.example.fragment.module.user.compose.UserScreen
import com.example.fragment.module.wan.compose.HomeScreen
import com.example.fragment.module.wan.compose.NavigationScreen
import com.example.fragment.module.wan.compose.ProjectScreen
import com.example.fragment.module.wan.compose.QAScreen
import com.example.fragment.module.wan.vm.*
import com.example.fragment.project.R
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.systemuicontroller.rememberSystemUiController

@Composable
fun MainScreen(
    viewModel: HotKeyViewModel = viewModel()
) {
    val statusBarColor = colorResource(com.example.fragment.module.user.R.color.theme)
    val systemUiController = rememberSystemUiController()
    SideEffect {
        systemUiController.setStatusBarColor(
            statusBarColor,
            darkIcons = false
        )
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val navItems = listOf(
        NavigationItem("首页", R.drawable.ic_bottom_bar_home, "home"),
        NavigationItem("导航", R.drawable.ic_bottom_bar_navigation, "navigation"),
        NavigationItem("问答", R.drawable.ic_bottom_bar_qa, "qa"),
        NavigationItem("项目", R.drawable.ic_bottom_bar_project, "project"),
        NavigationItem("我的", R.drawable.ic_bottom_bar_user, "user"),
    )
    var navRoute by remember { mutableStateOf(navItems[viewModel.getTabIndex()].route) }

    Scaffold(
        modifier = Modifier.systemBarsPadding(),
        topBar = {
            SearchBar(uiState.result)
        },
        bottomBar = {
            NavigationBar(
                items = navItems,
                selectedIndex = viewModel.getTabIndex(),
                onClick = { index, route ->
                    navRoute = route
                    viewModel.updateTabIndex(index)
                }
            )
        }
    ) { innerPadding ->
        Box(Modifier.padding(innerPadding)) {
            when (navRoute) {
                "home" -> HomeScreen()
                "navigation" -> NavigationScreen()
                "qa" -> QAScreen()
                "project" -> ProjectScreen()
                "user" -> UserScreen()
            }
        }
    }
}

@OptIn(ExperimentalPagerApi::class)
@Composable
fun SearchBar(
    data: List<HotKeyBean>?
) {
    if (data.isNullOrEmpty()) {
        return
    }

    val context = LocalContext.current
    var routerActivity: RouterActivity? = null
    if (context is RouterActivity) {
        routerActivity = context
    }

    Row(
        modifier = Modifier
            .background(colorResource(R.color.theme))
            .fillMaxWidth()
            .height(45.dp)
            .padding(15.dp, 8.dp, 15.dp, 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .background(colorResource(R.color.three_nine_gray), RoundedCornerShape(50))
                .weight(1f)
                .fillMaxHeight(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(Modifier.width(15.dp))
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                modifier = Modifier.padding(0.dp, 5.dp, 0.dp, 5.dp),
                tint = colorResource(R.color.white)
            )
            Spacer(Modifier.width(15.dp))
            LoopVerticalPager(
                data = data
            ) { _, _, item ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable {
                            routerActivity?.navigation(
                                Router.SEARCH,
                                bundleOf(Keys.VALUE to item.name)
                            )
                        },
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = item.name,
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
                    routerActivity?.navigation(Router.USER_SHARE)
                },
            tint = colorResource(R.color.white)
        )
    }
}

@Composable
fun NavigationBar(
    items: List<NavigationItem> = listOf(),
    selectedIndex: Int,
    onClick: (index: Int, route: String) -> Unit
) {
    var currItem by remember { mutableStateOf(selectedIndex) }

    BottomNavigation(
        modifier = Modifier,
        backgroundColor = colorResource(com.example.fragment.library.base.R.color.white)
    ) {
        items.forEachIndexed { index, item ->
            BottomNavigationItem(
                selected = currItem == index,
                onClick = {
                    currItem = index
                    onClick(index, item.route)
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
    val route: String,
    val selectedColor: Int = com.example.fragment.library.base.R.color.theme_orange,
    val unselectedColor: Int = com.example.fragment.library.base.R.color.theme
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
        NavigationItem("首页", R.drawable.ic_bottom_bar_home, "home"),
        NavigationItem("导航", R.drawable.ic_bottom_bar_navigation, "navigation"),
        NavigationItem("问答", R.drawable.ic_bottom_bar_qa, "qa"),
        NavigationItem("项目", R.drawable.ic_bottom_bar_project, "project"),
        NavigationItem("我的", R.drawable.ic_bottom_bar_user, "user"),
    )
    WanTheme { NavigationBar(items = navItems, selectedIndex = 0, onClick = { _, _ -> }) }
}