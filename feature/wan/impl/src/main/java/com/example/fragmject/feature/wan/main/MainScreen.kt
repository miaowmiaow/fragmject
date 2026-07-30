package com.example.fragmject.feature.wan.main

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
import androidx.compose.runtime.key
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
import com.example.fragmject.feature.wan.SearchNavKey
import com.example.fragmject.feature.wan.*
import com.example.fragmject.feature.wan.ShareArticleNavKey
import com.example.fragmject.core.designsystem.WanTheme
import com.example.fragmject.feature.wan.WanViewModel
import com.example.fragmject.core.ui.components.LoopVerticalPager
import com.example.fragmject.core.model.HotKey
import com.example.fragmject.core.model.Tree
import com.example.fragmject.core.designsystem.NavigationItem
import kotlinx.coroutines.launch
import com.example.fragmject.core.designsystem.WanColors
import com.example.fragmject.feature.wan.main.my.MyScreen
import com.example.fragmject.feature.wan.main.nav.NavScreen
import com.example.fragmject.feature.wan.main.home.HomeScreen
import com.example.fragmject.feature.wan.main.project.ProjectScreen
import com.example.fragmject.feature.wan.web.WebScreen
import com.example.fragmject.feature.wan.browse_history.BrowseHistoryScreen
import com.example.fragmject.feature.wan.my_coin.MyCoinScreen
import com.example.fragmject.feature.wan.my_collect.MyCollectScreen
import com.example.fragmject.feature.wan.my_share.MyShareScreen
import com.example.fragmject.feature.wan.rank.RankScreen
import com.example.fragmject.feature.wan.setting.SettingScreen
import com.example.fragmject.feature.wan.system.SystemScreen
import com.example.fragmject.feature.wan.user.UserScreen
import com.example.fragmject.core.designsystem.LocalWindowSizeClass

// =====================================================================
// 公共：渲染 Tab 内容
// =====================================================================

@Composable
private fun MainContent(
    navIndex: Int,
    navItems: List<NavigationItem>,
    systemData: List<Tree>,
    homeListState: LazyListState,
    onNavigate: (key: NavKey) -> Unit,
    modifier: Modifier = Modifier,
) {
    val saveableStateHolder = rememberSaveableStateHolder()
    Column(modifier = modifier) {
        when (navIndex) {
            0 -> saveableStateHolder.SaveableStateProvider(navItems[0].label) {
                HomeScreen(listState = homeListState, onNavigate = onNavigate)
            }

            1 -> saveableStateHolder.SaveableStateProvider(navItems[1].label) {
                NavScreen(systemData = systemData, onNavigate = onNavigate)
            }

            2 -> saveableStateHolder.SaveableStateProvider(navItems[2].label) {
                ProjectScreen(onNavigate = onNavigate)
            }

            3 -> saveableStateHolder.SaveableStateProvider(navItems[3].label) {
                MyScreen(onNavigate = onNavigate)
            }
        }
    }
}

// =====================================================================
// 主入口 — 按 WindowSizeClass 分发
// =====================================================================

@Composable
fun MainScreen(
    viewModel: WanViewModel = viewModel(),
    onNavigate: (key: NavKey) -> Unit = {},
    selectedDetailKey: NavKey? = null,
    onClearDetail: () -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val homeListState = rememberLazyListState()
    var navIndex by rememberSaveable { mutableIntStateOf(0) }
    val navItems = remember {
        listOf(
            NavigationItem("首页", R.mipmap.ic_bottom_bar_home),
            NavigationItem("导航", R.mipmap.ic_bottom_bar_navigation),
            NavigationItem("项目", R.mipmap.ic_bottom_bar_project),
            NavigationItem("我的", R.mipmap.ic_bottom_bar_user),
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
    navItems: List<NavigationItem>,
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
    navItems: List<NavigationItem>,
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
            Column(
                modifier = Modifier.width(IntrinsicSize.Min),
            ) {
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
        Column(modifier = Modifier.fillMaxSize()) {
            SearchBar(data = hotKeyResult, onNavigate = onNavigate)
            MainContent(
                navIndex,
                navItems,
                systemData,
                homeListState,
                onNavigate,
                Modifier.fillMaxSize()
            )
        }
    }
}

// =====================================================================
// Expanded — PermanentNavigationDrawer（平板横屏 / 桌面大屏）
// =====================================================================

@Composable
private fun ExpandedMainScreen(
    navItems: List<NavigationItem>,
    navIndex: Int,
    onNavClick: (Int) -> Unit,
    hotKeyResult: List<HotKey>?,
    systemData: List<Tree>,
    homeListState: LazyListState,
    onNavigate: (key: NavKey) -> Unit,
    selectedDetailKey: NavKey?,
    onClearDetail: () -> Unit,
) {
    PermanentNavigationDrawer(
        drawerContent = {
            Surface(
                modifier = Modifier.statusBarsPadding(),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp,
            ) {
                Column(
                    modifier = Modifier.width(IntrinsicSize.Min),
                ) {
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
        }
    ) {
        if (selectedDetailKey != null) {
            BackHandler { onClearDetail() }
            Row(modifier = Modifier.fillMaxSize()) {
                // ── 左侧：列表 (50%) ──
                Column(
                    modifier = Modifier
                        .weight(0.5f)
                        .fillMaxHeight()
                ) {
                    SearchBar(data = hotKeyResult, onNavigate = onNavigate)
                    MainContent(
                        navIndex,
                        navItems,
                        systemData,
                        homeListState,
                        onNavigate,
                        Modifier.fillMaxSize()
                    )
                }
                // ── 右侧：详情面板 (50%) ──
                Column(
                    modifier = Modifier
                        .weight(0.5f)
                        .fillMaxHeight()
                ) {
                    DetailPane(
                        navKey = selectedDetailKey,
                        onNavigate = onNavigate,
                        onClose = onClearDetail,
                    )
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                SearchBar(data = hotKeyResult, onNavigate = onNavigate)
                MainContent(
                    navIndex,
                    navItems,
                    systemData,
                    homeListState,
                    onNavigate,
                    Modifier.fillMaxSize()
                )
            }
        }
    }
}

// =====================================================================
// 详情面板 — 在 Expanded 模式下渲染右侧详情内容
// =====================================================================

/**
 * 根据 [navKey] 类型渲染对应的详情内容。
 *
 * 支持：
 * - [WebNavKey]             → WebScreen（文章详情）
 * - [UserNavKey]            → UserScreen（用户主页）
 * - [SystemNavKey]          → SystemScreen（体系文章列表）
 * - [SettingNavKey]         → SettingScreen（系统设置）
 * - [MyCoinNavKey]          → MyCoinScreen（我的积分）
 * - [MyCollectNavKey]       → MyCollectScreen（我的收藏）
 * - [MyShareNavKey]         → MyShareScreen（我的分享）
 * - [RankNavKey]            → RankScreen（积分排行）
 * - [BrowseHistoryNavKey]   → BrowseHistoryScreen（浏览历史）
 */
@Composable
private fun DetailPane(
    navKey: NavKey,
    onNavigate: (NavKey) -> Unit,
    onClose: () -> Unit,
) {
    when (navKey) {
        is WebNavKey -> key(navKey.url) {
            WebScreen(url = navKey.url, onNavigate = onNavigate, onNavigateUp = onClose)
        }
        is UserNavKey -> key(navKey.userId) {
            UserScreen(userId = navKey.userId, onNavigate = onNavigate, onNavigateUp = onClose)
        }
        is SystemNavKey -> key(navKey.cid) {
            SystemScreen(cid = navKey.cid, onNavigate = onNavigate, onNavigateUp = onClose)
        }
        is SettingNavKey -> {
            SettingScreen(onNavigate = onNavigate, onNavigateUp = onClose)
        }
        is MyCoinNavKey -> {
            MyCoinScreen(onNavigate = onNavigate, onNavigateUp = onClose)
        }
        is MyCollectNavKey -> {
            MyCollectScreen(onNavigate = onNavigate, onNavigateUp = onClose)
        }
        is MyShareNavKey -> {
            MyShareScreen(onNavigate = onNavigate, onNavigateUp = onClose)
        }
        is RankNavKey -> {
            RankScreen(onNavigate = onNavigate, onNavigateUp = onClose)
        }
        is BrowseHistoryNavKey -> {
            BrowseHistoryScreen(onNavigate = onNavigate, onNavigateUp = onClose)
        }
    }
}

// =====================================================================
// 公共：导航图标
// =====================================================================

@Composable
private fun NavIcon(item: NavigationItem) {
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
                .background(WanColors.alphaGray)
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
    WanTheme { MainScreen() }
}

@Preview(showBackground = true, backgroundColor = 0xFFF0F0F0)
@Composable
fun SearchBarPreview() {
    WanTheme { SearchBar(data = listOf(HotKey(name = "问答"))) }
}

@Preview(showBackground = true, backgroundColor = 0xFFF0F0F0)
@Composable
fun WanBottomNavigationPreview() {
    val navItems = listOf(
        NavigationItem("首页", R.mipmap.ic_bottom_bar_home),
        NavigationItem("导航", R.mipmap.ic_bottom_bar_navigation),
        NavigationItem("项目", R.mipmap.ic_bottom_bar_project),
        NavigationItem("我的", R.mipmap.ic_bottom_bar_user),
    )
    WanTheme {
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