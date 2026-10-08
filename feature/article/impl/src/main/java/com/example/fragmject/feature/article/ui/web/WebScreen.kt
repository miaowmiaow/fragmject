package com.example.fragmject.feature.article.ui.web

import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetValue
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fragmject.core.designsystem.AppTheme
import com.example.fragmject.core.designsystem.TitleBar
import com.example.fragmject.core.navigation.contract.LocalArticleNavigator
import com.example.fragmject.core.navigation.contract.LocalUserNavigator
import com.example.fragmject.core.navigation.runtime.LocalOnNavigateUp
import com.example.fragmject.core.ui.R
import com.example.fragmject.core.webview.rememberWebViewControl
import com.example.fragmject.feature.article.components.ArticleWebViewContainer
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebScreen(
    url: String,
) {
    val context = LocalContext.current
    val userNavigator = LocalUserNavigator.current
    val articleNavigator = LocalArticleNavigator.current
    val onNavigateUp = LocalOnNavigateUp.current
    val scope = rememberCoroutineScope()
    val webViewModel: WebViewModel = viewModel()
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
    val control = rememberWebViewControl()
    val mediaController = rememberWebMediaController()
    var injectState by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf<String?>("") }
    val bookmark by webViewModel.bookmark.collectAsStateWithLifecycle()
    LaunchedEffect(url) { webViewModel.init(url) }
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

    // 全屏视频：进入全屏时独占渲染，不再渲染 Scaffold/Sheet/进度条等下层内容
    val fullScreenView = customView
    if (fullScreenView != null) {
        AndroidView(
            factory = { fullScreenView },
            modifier = Modifier.fillMaxSize(),
        )
        return
    }

    Scaffold(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .navigationBarsPadding(),
        topBar = {
            TitleBar(
                title = title.toString(),
                navigationIcon = {
                    IconButton(
                        onClick = {
                            onNavigateUp()
                        },
                        modifier = Modifier.height(45.dp)
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
    ) { innerPadding ->
        Box(
            modifier = Modifier.padding(innerPadding)
        ) {
            WebActionSheet(
                bottomSheetState = bottomSheetState,
                bookmark = bookmark,
                injectState = injectState,
                onReload = {
                    control.reload()
                    scope.launch { bottomSheetState.partialExpand() }
                },
                onOpenHistory = {
                    userNavigator.openBrowseHistory()
                    scope.launch { bottomSheetState.partialExpand() }
                },
                onToggleBookmark = {
                    webViewModel.toggleBookmark(title.toString(), url)
                    scope.launch { bottomSheetState.partialExpand() }
                },
                onToggleInject = {
                    injectState = !injectState
                    control.reload()
                    scope.launch { bottomSheetState.partialExpand() }
                },
            ) { padding ->
                WebProgressBar(progress = control.progress)
                ArticleWebViewContainer(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    url = url,
                    control = control,
                    injectState = injectState,
                    onReceivedTitle = {
                        title = it
                        webViewModel.recordBrowseVisit(it.toString(), url)
                    },
                    onCustomView = { customView = it },
                    shouldOverrideUrl = { articleNavigator.openArticle(it) },
                    onLongPressImage = mediaController::onLongPressImage,
                )
                WebMediaDialogs(
                    controller = mediaController,
                    onSaveImage = webViewModel::saveImage,
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF0F0F0)
@Composable
fun WebScreenPreview() {
    AppTheme { WebScreen(url = "https://wanandroid.com/") }
}