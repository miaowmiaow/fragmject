package com.example.fragmject.feature.article.ui.web

import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetState
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.Stable
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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.fragmject.core.designsystem.AppTheme
import com.example.fragmject.core.ui.components.TitleBar
import com.example.fragmject.core.navigation.contract.LocalNavigators
import com.example.fragmject.core.navigation.runtime.LocalOnNavigateUp
import com.example.fragmject.feature.article.impl.R
import com.example.fragmject.core.webview.WebViewControl
import com.example.fragmject.core.webview.di.WebViewPoolEntryPoint
import com.example.fragmject.core.webview.rememberWebViewControl
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebScreen(
    url: String,
) {
    val userNavigator = LocalNavigators.current.user
    val articleNavigator = LocalNavigators.current.article
    val onNavigateUp = LocalOnNavigateUp.current
    val scope = rememberCoroutineScope()
    val webViewModel: WebViewModel = hiltViewModel()
    var customView by remember { mutableStateOf<View?>(null) }
    var sheetValue by rememberSaveable { mutableStateOf(SheetValue.PartiallyExpanded) }
    // initialValue 只参与首次创建：跨重建的停靠位置由 rememberSaveable 的 sheetValue 回填，
    // confirmValueChange 负责把后续变化写回去。enabledValues 用默认值（Hidden/半展开/展开），
    // 与旧 rememberStandardBottomSheetState(skipHiddenState = false) 等价。
    val bottomSheetState = rememberBottomSheetState(
        initialValue = sheetValue,
        confirmValueChange = {
            sheetValue = it
            true
        },
    )
    val control = rememberWebViewControl()
    val mediaController = rememberWebMediaController()
    var injectState by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf<String?>("") }
    val bookmark by webViewModel.bookmark.collectAsStateWithLifecycle()
    LaunchedEffect(url) { webViewModel.init(url) }
    WebSystemBarsEffect(customView = customView)

    val snackbarHostState = rememberCredentialedCacheNotice()

    // 全屏视频：进入全屏时独占渲染，不再渲染 Scaffold/Sheet/进度条等下层内容
    val fullScreenView = customView
    if (fullScreenView != null) {
        AndroidView(factory = { fullScreenView }, modifier = Modifier.fillMaxSize())
        return
    }

    // 面板上每个动作执行后都收回半屏
    val collapseSheet: () -> Unit = { scope.launch { bottomSheetState.partialExpand() } }
    Scaffold(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .navigationBarsPadding(),
        snackbarHost = { SnackbarHost(snackbarHostState) { data -> Snackbar(snackbarData = data) } },
        topBar = {
            WebTopBar(title.toString(), sheetValue, bottomSheetState, scope, onNavigateUp)
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier.padding(innerPadding)
        ) {
            WebActionSheetHost(
                bottomSheetState = bottomSheetState,
                pageState = WebActionState(bookmark, injectState),
                actions = WebSheetActions(
                    onReload = { control.reload(); collapseSheet() },
                    onOpenHistory = { userNavigator.openBrowseHistory(); collapseSheet() },
                    onToggleBookmark = { webViewModel.toggleBookmark(title.toString(), url); collapseSheet() },
                    onToggleInject = { injectState = !injectState; control.reload(); collapseSheet() },
                ),
            ) { padding ->
                ArticleWebBody(
                    page = WebPageResources(
                        url = url,
                        control = control,
                        mediaController = mediaController,
                    ),
                    injectState = injectState,
                    padding = padding,
                    onReceivedTitle = {
                        title = it
                        webViewModel.recordBrowseVisit(it.toString(), url)
                    },
                    onCustomView = { customView = it },
                    shouldOverrideUrl = { articleNavigator.openArticle(it) },
                    onSaveImage = webViewModel::saveImage,
                )
            }
        }
    }
}

/** 半屏面板的动作回调集合。 */
@Stable
private class WebSheetActions(
    val onReload: () -> Unit,
    val onOpenHistory: () -> Unit,
    val onToggleBookmark: () -> Unit,
    val onToggleInject: () -> Unit,
)

/** 网页运行时资源：地址、控制通道与媒体控制器。 */
@Stable
private class WebPageResources(
    val url: String,
    val control: WebViewControl,
    val mediaController: WebMediaController,
)

/** 半屏操作面板：只做参数展开，状态与编排留在屏幕层。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WebActionSheetHost(
    bottomSheetState: SheetState,
    pageState: WebActionState,
    actions: WebSheetActions,
    content: @Composable (PaddingValues) -> Unit,
) {
    WebActionSheet(
        bottomSheetState = bottomSheetState,
        pageState = pageState,
        onReload = actions.onReload,
        onOpenHistory = actions.onOpenHistory,
        onToggleBookmark = actions.onToggleBookmark,
        onToggleInject = actions.onToggleInject,
        content = content,
    )
}

/** 面板内容：进度条 + 网页容器 + 媒体对话框。 */
@Composable
private fun ArticleWebBody(
    page: WebPageResources,
    injectState: Boolean,
    padding: PaddingValues,
    onReceivedTitle: (String?) -> Unit,
    onCustomView: (View?) -> Unit,
    shouldOverrideUrl: (String) -> Unit,
    onSaveImage: (extra: String?, onResult: (Boolean) -> Unit) -> Unit,
) {
    WebProgressBar(progress = page.control.progress)
    ArticleWebViewContainer(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        url = page.url,
        control = page.control,
        injectState = injectState,
        onReceivedTitle = onReceivedTitle,
        onCustomView = onCustomView,
        shouldOverrideUrl = shouldOverrideUrl,
        onLongPressImage = page.mediaController::onLongPressImage,
    )
    WebMediaDialogs(
        controller = page.mediaController,
        onSaveImage = onSaveImage,
    )
}

/**
 * 首次用登录态缓存第三方资源时的一次性轻提示，返回供 Scaffold 使用的 [SnackbarHostState]。
 *
 * 为什么不是「逐资源弹窗」：缓存填充是 `shouldInterceptRequest` 的异步旁路，一篇文章页
 * 有几十个资源，逐次询问会把阅读变成问卷，且弹窗时机与下载时序不可控。
 * 选择权保留在设置页（「用登录态缓存图片」开关），这里只做一次知情提示。
 */
@Composable
private fun rememberCredentialedCacheNotice(): SnackbarHostState {
    val context = LocalContext.current
    val cacheSettings = remember(context) {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            WebViewPoolEntryPoint::class.java,
        ).webCacheSettings()
    }
    val snackbarHostState = remember { SnackbarHostState() }
    val notice by cacheSettings.credentialedNotice.collectAsStateWithLifecycle()
    LaunchedEffect(notice) {
        if (!notice) return@LaunchedEffect
        cacheSettings.consumeCredentialedNotice()
        snackbarHostState.showSnackbar("已用登录态缓存第三方图片，可在设置中关闭")
    }
    return snackbarHostState
}

/** 顶栏：标题 + 返回 + 「更多」面板开关（抽出以免 WebScreen 超过 LongMethod 上限）。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WebTopBar(
    title: String,
    sheetValue: SheetValue,
    bottomSheetState: SheetState,
    scope: CoroutineScope,
    onNavigateUp: () -> Unit,
) {
    TitleBar(
        title = title,
        onNavigateUp = onNavigateUp,
        actions = { WebSheetToggleAction(sheetValue, bottomSheetState, scope) },
    )
}

/** 全屏视频期间隐藏系统栏，退出全屏时恢复。 */
@Composable
private fun WebSystemBarsEffect(customView: View?) {
    val context = LocalContext.current
    DisposableEffect(customView) {
        val activity = context as? ComponentActivity
        val insetsController = activity?.window?.let {
            WindowCompat.getInsetsController(it, it.decorView)
        }
        if (customView != null && insetsController != null) {
            insetsController.hide(WindowInsetsCompat.Type.systemBars())
        }
        onDispose {
            insetsController?.show(WindowInsetsCompat.Type.systemBars())
        }
    }
}

/** 顶栏右侧「更多」：在部分展开与完全展开之间切换。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WebSheetToggleAction(
    sheetValue: SheetValue,
    bottomSheetState: SheetState,
    scope: CoroutineScope,
) {
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
}

@Preview(showBackground = true, backgroundColor = 0xFFF0F0F0)
@Composable
fun WebScreenPreview() {
    AppTheme { WebScreen(url = "https://wanandroid.com/") }
}