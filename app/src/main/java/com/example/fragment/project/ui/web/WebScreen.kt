package com.example.fragment.project.ui.web

import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.example.fragment.project.R
import com.example.fragment.project.WanTheme
import com.example.fragment.project.components.TitleBar
import com.example.fragment.project.data.NavigationItem
import com.example.fragment.project.database.history.History
import com.example.fragment.project.utils.WanHelper
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebScreen(
    url: String,
    onNavigateToBookmarkHistory: () -> Unit = {},
    onNavigateUp: () -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val gridState = rememberLazyGridState()
    val navItems = listOf(
        NavigationItem("返回", R.mipmap.ic_web_back),
        NavigationItem("前进", R.mipmap.ic_web_forward),
        NavigationItem("刷新", R.mipmap.ic_web_refresh),
        NavigationItem("更多", R.mipmap.ic_web_more),
        NavigationItem("外部浏览", R.mipmap.ic_web_browse),
        NavigationItem("浏览历史", R.mipmap.ic_web_history),
        NavigationItem("保存书签", R.mipmap.ic_web_bookmark),
        NavigationItem("调试模式", R.mipmap.ic_web_debug),
    )
    var customView by remember { mutableStateOf<View?>(null) }
    var sheetValue by rememberSaveable { mutableStateOf(SheetValue.PartiallyExpanded) }
    val bottomSheetState = rememberStandardBottomSheetState(
        initialValue = sheetValue,
        confirmValueChange = {
            sheetValue = it
            true
        },
        skipHiddenState = false
    )
    val scaffoldState = rememberBottomSheetScaffoldState(bottomSheetState)
    val navigator = rememberWebViewNavigator()
    var bookmark by remember { mutableStateOf<History?>(null) }
    LaunchedEffect(navigator.loadedUrl) {
        WanHelper.getBookmark().collect {
            WanHelper.getBookmark().collect { bk ->
                bookmark = bk.firstOrNull { it.url == navigator.loadedUrl }
            }
        }
    }
    DisposableEffect(customView) {
        val activity = context as ComponentActivity
        val window = activity.window
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        if (customView != null) {
            insetsController.hide(WindowInsetsCompat.Type.systemBars())
        }
        onDispose {
            insetsController.show(WindowInsetsCompat.Type.systemBars())
        }
    }
    Box {
        Scaffold(
            topBar = {
                TitleBar(
                    title = navigator.title ?: "",
                    navigationIcon = {
                        IconButton(
                            modifier = Modifier.height(45.dp),
                            onClick = {
                                scope.launch { navigator.navigateBack() }
                            }
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                scope.launch {
                                    if (sheetValue == SheetValue.PartiallyExpanded) {
                                        bottomSheetState.expand()
                                    } else {
                                        bottomSheetState.partialExpand()
                                    }
                                }
                            }
                        ) {
                            Icon(
                                painter = painterResource(R.mipmap.ic_more_v),
                                contentDescription = null,
                                modifier = Modifier.padding(8.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    })
            },
            contentWindowInsets = WindowInsets.statusBars
        ) { innerPadding ->
            BottomSheetScaffold(
                sheetContent = {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(128.dp),
                        state = gridState,
                        userScrollEnabled = false,
                        content = {
                            items(navItems, key = { it.label }) { nav ->
                                Button(
                                    onClick = {
                                        when (nav.resId) {
                                            R.mipmap.ic_web_back -> {
                                                navigator.navigateBack()
                                            }

                                            R.mipmap.ic_web_forward -> {
                                                navigator.navigateForward()
                                            }

                                            R.mipmap.ic_web_refresh -> {
                                                navigator.reload()
                                                scope.launch { bottomSheetState.partialExpand() }
                                            }

                                            R.mipmap.ic_web_more -> {
                                                scope.launch {
                                                    if (sheetValue == SheetValue.PartiallyExpanded) {
                                                        bottomSheetState.expand()
                                                    } else {
                                                        bottomSheetState.partialExpand()
                                                    }
                                                }
                                            }

                                            R.mipmap.ic_web_browse -> {
                                                try {
                                                    val uri = Uri.parse(navigator.loadedUrl)
                                                    val intent = Intent(Intent.ACTION_VIEW, uri)
                                                    intent.addCategory(Intent.CATEGORY_BROWSABLE)
                                                    context.startActivity(intent)
                                                    scope.launch { bottomSheetState.partialExpand() }
                                                } catch (e: Exception) {
                                                    Log.e(
                                                        this.javaClass.name,
                                                        e.message.toString()
                                                    )
                                                }
                                            }

                                            R.mipmap.ic_web_history -> {
                                                onNavigateToBookmarkHistory()
                                                scope.launch { bottomSheetState.partialExpand() }
                                            }

                                            R.mipmap.ic_web_bookmark -> {
                                                scope.launch {
                                                    if (bookmark != null) {
                                                        WanHelper.deleteHistory(bookmark!!)
                                                    } else {
                                                        WanHelper.setBookmark(
                                                            navigator.title.toString(),
                                                            navigator.loadedUrl.toString()
                                                        )
                                                    }
                                                }
                                            }

                                            R.mipmap.ic_web_debug -> {
                                                navigator.injectVConsole()
                                                scope.launch { bottomSheetState.partialExpand() }
                                            }
                                        }
                                    },
                                    modifier = Modifier.height(64.dp),
                                    shape = RoundedCornerShape(0),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp),
                                    contentPadding = PaddingValues(20.dp),
                                ) {
                                    Icon(
                                        painter = painterResource(nav.resId),
                                        contentDescription = null,
                                        tint = if (bookmark != null && nav.resId == R.mipmap.ic_web_bookmark) {
                                            MaterialTheme.colorScheme.onSurface
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                }
                            }
                        }
                    )
                },
                modifier = Modifier.padding(innerPadding),
                scaffoldState = scaffoldState,
                sheetPeekHeight = 0.dp,
                sheetShape = RoundedCornerShape(0.dp),
                sheetShadowElevation = 10.dp,
                sheetDragHandle = null,
                sheetSwipeEnabled = false
            ) { padding ->
                AnimatedVisibility(visible = (navigator.progress > 0f && navigator.progress < 1f)) {
                    LinearProgressIndicator(
                        progress = { navigator.progress },
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.onSurface,
                        trackColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                }
                WebViewNavGraph(
                    url = url,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    onCustomView = { customView = it },
                    onReceivedTitle = { url, title ->
                        scope.launch {
                            WanHelper.setBrowseHistory(title.toString(), url.toString())
                        }
                    },
                    navigator = navigator,
                    navigateUp = onNavigateUp,
                )
            }
        }
        customView?.let {
            AndroidView(factory = { _ -> it })
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF0F0F0)
@Composable
fun WebScreenPreview() {
    WanTheme { WebScreen(url = "https://wanandroid.com/") }
}