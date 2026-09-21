package com.example.fragmject.feature.home.ui.main

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.PermanentNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import com.example.fragmject.core.ui.R
import com.example.fragmject.feature.search.SearchNavKey
import com.example.fragmject.feature.collection.ShareArticleNavKey
import com.example.fragmject.core.designsystem.AppTheme
import com.example.fragmject.core.ui.components.LoopVerticalPager
import com.example.fragmject.core.model.HotKey
import com.example.fragmject.core.model.Tree
import com.example.fragmject.core.designsystem.BottomNavItem
import kotlinx.coroutines.launch
import com.example.fragmject.core.designsystem.AppColors
import com.example.fragmject.feature.home.ui.my.MyScreen
import com.example.fragmject.feature.home.ui.nav.NavScreen
import com.example.fragmject.feature.home.ui.home.HomeScreen
import com.example.fragmject.feature.home.ui.project.ProjectScreen
import com.example.fragmject.feature.home.nav.homeNavActions
import com.example.fragmject.feature.home.nav.myNavActions
import com.example.fragmject.core.designsystem.LocalWindowSizeClass

// =====================================================================
// 公共：渲染 Tab 内容
// =====================================================================

@Composable
private fun MainContent(
    navIndex: Int,
    navItems: List<BottomNavItem>,
    systemData: List<Tree>,
    homeListState: LazyListState,
    onNavigate: (key: NavKey) -> Unit,
    modifier: Modifier = Modifier,
) {
    val saveableStateHolder = rememberSaveableStateHolder()
    Column(modifier = modifier) {
        when (navIndex) {
            0 -> saveableStateHolder.SaveableStateProvider(navItems[0].label) {
                HomeScreen(listState = homeListState, actions = homeNavActions(onNavigate))
            }

            1 -> saveableStateHolder.SaveableStateProvider(navItems[1].label) {
                NavScreen(systemData = systemData, actions = homeNavActions(onNavigate))
            }

            2 -> saveableStateHolder.SaveableStateProvider(navItems[2].label) {
                ProjectScreen(actions = homeNavActions(onNavigate))
            }

            3 -> saveableStateHolder.SaveableStateProvider(navItems[3].label) {
                MyScreen(actions = myNavActions(onNavigate))
            }
        }
    }
}

// =====================================================================
// 公共：抽屉 / 导航栏的导航项
// =====================================================================

@Composable
private fun NavigationDrawerContent(
    navItems: List<BottomNavItem>,
    navIndex: Int,
    onNavClick: (Int) -> Unit,
) {
    Column(modifier = Modifier.width(IntrinsicSize.Min)) {
        Spacer(Modifier.height(45.dp))
        navItems.forEachIndexed { index, item ->
            NavigationDrawerItem(
                selected = navIndex == index,
                onClick = { onNavClick(index) },
                icon = { NavIcon(item) },
                label = {
                    Text(
                        text = item.label,
                        modifier = Modifier.width(IntrinsicSize.Max),
                        fontSize = 13.sp,
                        lineHeight = 13.sp
                    )
                },
            )
        }
    }
}

// =====================================================================
// 公共：SearchBar + Tab 内容
// =====================================================================

@Composable
private fun ContentPane(
    navItems: List<BottomNavItem>,
    navIndex: Int,
    hotKeyResult: List<HotKey>?,
    systemData: List<Tree>,
    homeListState: LazyListState,
    onNavigate: (key: NavKey) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        SearchBar(data = hotKeyResult, onNavigate = onNavigate)
        MainContent(
            navIndex = navIndex,
            navItems = navItems,
            systemData = systemData,
            homeListState = homeListState,
            onNavigate = onNavigate,
            modifier = Modifier.fillMaxSize()
        )
    }
}

// =====================================================================
// 主入口 — 按 WindowSizeClass 分发
// =====================================================================

@Composable
fun MainScreen(
    viewModel: MainViewModel = viewModel(),
    onNavigate: (key: NavKey) -> Unit = {},
    selectedDetailKey: NavKey? = null,
    onClearDetail: () -> Unit = {},
    detailContent: @Composable (NavKey, (NavKey) -> Unit, () -> Unit) -> Unit = { _, _, _ -> },
) {
    val scope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val homeListState = rememberLazyListState()

    var navIndex by rememberSaveable { mutableIntStateOf(0) }
    val navItems = remember {
        listOf(
            BottomNavItem("首页", R.mipmap.ic_bottom_bar_home),
            BottomNavItem("导航", R.mipmap.ic_bottom_bar_navigation),
            BottomNavItem("项目", R.mipmap.ic_bottom_bar_project),
            BottomNavItem("我的", R.mipmap.ic_bottom_bar_user),
        )
    }

    val onNavClick: (Int) -> Unit = { index ->
        if ((index == 0) && (navIndex == 0) && homeListState.canScrollBackward) {
            scope.launch { homeListState.animateScrollToItem(0) }
        }
        navIndex = index
    }

    val windowSizeClass = LocalWindowSizeClass.current

    when (windowSizeClass.widthSizeClass) {
        WindowWidthSizeClass.Expanded -> ExpandedMainScreen(
            navItems, navIndex, onNavClick,
            uiState.hotKeyResult, uiState.treeResult,
            homeListState, onNavigate,
            selectedDetailKey = selectedDetailKey,
            onClearDetail = onClearDetail,
            detailContent = detailContent,
        )

        WindowWidthSizeClass.Medium -> MediumMainScreen(
            navItems, navIndex, onNavClick,
            uiState.hotKeyResult, uiState.treeResult,
            homeListState, onNavigate,
        )

        else -> CompactMainScreen(
            navItems, navIndex, onNavClick,
            uiState.hotKeyResult, uiState.treeResult,
            homeListState, onNavigate,
        )
    }
}

// =====================================================================
// Compact — NavigationBar（手机竖屏）
// =====================================================================

@Composable
private fun CompactMainScreen(
    navItems: List<BottomNavItem>,
    navIndex: Int,
    onNavClick: (Int) -> Unit,
    hotKeyResult: List<HotKey>?,
    systemData: List<Tree>,
    homeListState: LazyListState,
    onNavigate: (key: NavKey) -> Unit,
) {
    Scaffold(
        topBar = { SearchBar(data = hotKeyResult, onNavigate = onNavigate) },
        bottomBar = {
            NavigationBar(modifier = Modifier.shadow(5.dp)) {
                navItems.forEachIndexed { index, item ->
                    NavigationBarItem(
                        selected = navIndex == index,
                        onClick = { onNavClick(index) },
                        icon = { NavIcon(item) },
                        label = { Text(item.label, fontSize = 13.sp, lineHeight = 13.sp) },
                    )
                }
            }
        }
    ) { innerPadding ->
        MainContent(
            navIndex,
            navItems,
            systemData,
            homeListState,
            onNavigate,
            Modifier.padding(innerPadding)
        )
    }
}

// =====================================================================
// Medium — NavigationRail（平板竖屏 / 折叠屏展开态）
// =====================================================================

@Composable
private fun MediumMainScreen(
    navItems: List<BottomNavItem>,
    navIndex: Int,
    onNavClick: (Int) -> Unit,
    hotKeyResult: List<HotKey>?,
    systemData: List<Tree>,
    homeListState: LazyListState,
    onNavigate: (key: NavKey) -> Unit,
) {
    Row(modifier = Modifier.fillMaxSize()) {
        Surface(
            modifier = Modifier
                .fillMaxHeight()
                .statusBarsPadding()
                .shadow(5.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
        ) {
            NavigationDrawerContent(navItems, navIndex, onNavClick)
        }
        ContentPane(
            navItems = navItems,
            navIndex = navIndex,
            hotKeyResult = hotKeyResult,
            systemData = systemData,
            homeListState = homeListState,
            onNavigate = onNavigate,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

// =====================================================================
// Expanded — PermanentNavigationDrawer（平板横屏 / 桌面大屏）
// =====================================================================

@Composable
private fun ExpandedMainScreen(
    navItems: List<BottomNavItem>,
    navIndex: Int,
    onNavClick: (Int) -> Unit,
    hotKeyResult: List<HotKey>?,
    systemData: List<Tree>,
    homeListState: LazyListState,
    onNavigate: (key: NavKey) -> Unit,
    selectedDetailKey: NavKey?,
    onClearDetail: () -> Unit,
    detailContent: @Composable (NavKey, (NavKey) -> Unit, () -> Unit) -> Unit,
) {
    PermanentNavigationDrawer(
        drawerContent = {
            Surface(
                modifier = Modifier.statusBarsPadding(),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp,
            ) {
                NavigationDrawerContent(navItems, navIndex, onNavClick)
            }
        }
    ) {
        if (selectedDetailKey != null) {
            BackHandler { onClearDetail() }
            Row(modifier = Modifier.fillMaxSize()) {
                // ── 左侧：列表 (50%) ──
                ContentPane(
                    navItems = navItems,
                    navIndex = navIndex,
                    hotKeyResult = hotKeyResult,
                    systemData = systemData,
                    homeListState = homeListState,
                    onNavigate = onNavigate,
                    modifier = Modifier.weight(0.5f).fillMaxHeight(),
                )
                // ── 右侧：详情面板 (50%) ──
                Column(
                    modifier = Modifier
                        .weight(0.5f)
                        .fillMaxHeight()
                ) {
                    detailContent(selectedDetailKey, onNavigate, onClearDetail)
                }
            }
        } else {
            ContentPane(
                navItems = navItems,
                navIndex = navIndex,
                hotKeyResult = hotKeyResult,
                systemData = systemData,
                homeListState = homeListState,
                onNavigate = onNavigate,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

// =====================================================================
// 公共：导航图标
// =====================================================================

@Composable
private fun NavIcon(item: BottomNavItem) {
    BadgedBox(
        badge = {
            if ("我的" == item.label) {
                Badge {
                    val badgeNumber = "1"
                    Text(
                        badgeNumber,
                        modifier = Modifier.semantics {
                            contentDescription = "$badgeNumber new notifications"
                        },
                    )
                }
            }
        },
    ) {
        Icon(
            painter = painterResource(id = item.resId),
            contentDescription = null,
            modifier = Modifier.size(25.dp),
        )
    }
}

// =====================================================================
// SearchBar
// =====================================================================

@Composable
fun SearchBar(
    data: List<HotKey>?,
    onNavigate: (key: NavKey) -> Unit = {},
) {
    Row(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.primaryContainer)
            .statusBarsPadding()
            .fillMaxWidth()
            .height(45.dp)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(Modifier.width(15.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .clipToBounds()
                .background(AppColors.alphaGray)
                .weight(1f)
                .fillMaxHeight(),
            contentAlignment = Alignment.CenterStart,
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                modifier = Modifier.padding(10.dp, 5.dp, 0.dp, 5.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            LoopVerticalPager(data = data, userScrollEnabled = false) { _, _, item ->
                Box(
                    modifier = Modifier
                        .clickable { onNavigate(SearchNavKey(item.name)) }
                        .fillMaxSize(),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Text(
                        text = item.name,
                        modifier = Modifier.padding(start = 40.dp),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        }
        IconButton(
            modifier = Modifier.height(45.dp),
            onClick = { onNavigate(ShareArticleNavKey) },
        ) {
            Icon(
                Icons.Filled.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

// =====================================================================
// Previews
// =====================================================================

@Preview(showBackground = true, backgroundColor = 0xFFF0F0F0)
@Composable
fun MainScreenPreview() {
    AppTheme { MainScreen() }
}

@Preview(showBackground = true, backgroundColor = 0xFFF0F0F0)
@Composable
fun SearchBarPreview() {
    AppTheme { SearchBar(data = listOf(HotKey(name = "问答"))) }
}

@Preview(showBackground = true, backgroundColor = 0xFFF0F0F0)
@Composable
fun BottomNavigationPreview() {
    val navItems = listOf(
        BottomNavItem("首页", R.mipmap.ic_bottom_bar_home),
        BottomNavItem("导航", R.mipmap.ic_bottom_bar_navigation),
        BottomNavItem("项目", R.mipmap.ic_bottom_bar_project),
        BottomNavItem("我的", R.mipmap.ic_bottom_bar_user),
    )
    AppTheme {
        NavigationBar(modifier = Modifier.shadow(5.dp)) {
            navItems.forEachIndexed { index, item ->
                NavigationBarItem(
                    selected = index == 0,
                    onClick = { },
                    icon = { NavIcon(item) },
                    label = { Text(item.label, fontSize = 13.sp, lineHeight = 13.sp) },
                )
            }
        }
    }
}