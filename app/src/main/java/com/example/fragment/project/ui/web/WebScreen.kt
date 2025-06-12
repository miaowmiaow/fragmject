package com.example.fragment.project.ui.web

import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.material3.Text
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
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.example.fragment.project.R
import com.example.fragment.project.WanTheme
import com.example.fragment.project.components.TitleBar
import com.example.fragment.project.data.History
import com.example.fragment.project.utils.WanHelper
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebScreen(
    url: String,
    onNavigateToBookmarkHistory: () -> Unit = {},
    onNavigateUp: () -> Unit = {},
    shouldOverrideUrl: (url: String) -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
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
    val control = rememberWebViewControl()
    var title by remember { mutableStateOf<String?>("") }
    var bookmark by remember { mutableStateOf<History?>(null) }
    LaunchedEffect(url) {
        WanHelper.getBookmark().collect {
            WanHelper.getBookmark().collect { bk ->
                bookmark = bk.firstOrNull { it.url == url }
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
            contentWindowInsets = WindowInsets.statusBars
        ) { innerPadding ->
            BottomSheetScaffold(
                sheetContent = {
                    HorizontalPager(
                        state = rememberPagerState(0) { 2 },
                    ) { page ->
                        if (page == 0) {
                            Row(modifier = Modifier.height(64.dp)) {
                                Button(
                                    onClick = {
                                        control.reload()
                                        scope.launch { bottomSheetState.partialExpand() }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                    shape = RoundedCornerShape(0),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp),
                                    contentPadding = PaddingValues(
                                        horizontal = 28.dp,
                                        vertical = 18.dp
                                    ),
                                ) {
                                    Icon(
                                        painter = painterResource(R.mipmap.ic_web_refresh),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Button(
                                    onClick = {
                                        onNavigateToBookmarkHistory()
                                        scope.launch { bottomSheetState.partialExpand() }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                    shape = RoundedCornerShape(0),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp),
                                    contentPadding = PaddingValues(
                                        horizontal = 28.dp,
                                        vertical = 18.dp
                                    ),
                                ) {
                                    Icon(
                                        painter = painterResource(R.mipmap.ic_web_history),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Button(
                                    onClick = {
                                        scope.launch {
                                            if (bookmark != null) {
                                                WanHelper.deleteHistory(bookmark!!)
                                            } else {
                                                WanHelper.setBookmark(title.toString(), url)
                                            }
                                            bottomSheetState.partialExpand()
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                    shape = RoundedCornerShape(0),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp),
                                    contentPadding = PaddingValues(
                                        horizontal = 28.dp,
                                        vertical = 18.dp
                                    ),
                                ) {
                                    Icon(
                                        painter = painterResource(R.mipmap.ic_web_bookmark),
                                        contentDescription = null,
                                        tint = if (bookmark != null) {
                                            MaterialTheme.colorScheme.onSurface
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                }
                                Button(
                                    onClick = {
                                        control.inject()
                                        scope.launch { bottomSheetState.partialExpand() }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                    shape = RoundedCornerShape(0),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp),
                                    contentPadding = PaddingValues(
                                        horizontal = 28.dp,
                                        vertical = 18.dp
                                    ),
                                ) {
                                    Icon(
                                        painter = painterResource(R.mipmap.ic_web_debug),
                                        contentDescription = null,
                                        tint = if (control.injectState) {
                                            MaterialTheme.colorScheme.onSurface
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                }
                            }
                        } else {
                            Row(modifier = Modifier.height(64.dp)) {
                                Button(
                                    onClick = {
                                        control.evaluateJavascript("javascript:quickBack5()")
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                    shape = RoundedCornerShape(0),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp),
                                    contentPadding = PaddingValues(
                                        horizontal = 18.dp,
                                        vertical = 18.dp
                                    ),
                                ) {
                                    Icon(
                                        painter = painterResource(R.mipmap.ic_quick_back),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(text = "5")
                                }
                                Button(
                                    onClick = {
                                        control.evaluateJavascript("javascript:quickBack10()")
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                    shape = RoundedCornerShape(0),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp),
                                    contentPadding = PaddingValues(
                                        horizontal = 18.dp,
                                        vertical = 18.dp
                                    ),
                                ) {
                                    Icon(
                                        painter = painterResource(R.mipmap.ic_quick_back),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(text = "10")
                                }
                                Button(
                                    onClick = {
                                        control.evaluateJavascript("javascript:quickForward5()")
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                    shape = RoundedCornerShape(0),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp),
                                    contentPadding = PaddingValues(
                                        horizontal = 18.dp,
                                        vertical = 18.dp
                                    ),
                                ) {
                                    Icon(
                                        painter = painterResource(R.mipmap.ic_quick_forward),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(text = "5")
                                }
                                Button(
                                    onClick = {
                                        control.evaluateJavascript("javascript:quickForward10()")
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                    shape = RoundedCornerShape(0),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp),
                                    contentPadding = PaddingValues(
                                        horizontal = 18.dp,
                                        vertical = 18.dp
                                    ),
                                ) {
                                    Icon(
                                        painter = painterResource(R.mipmap.ic_quick_forward),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(text = "10")
                                }
                            }
                        }
                    }
                },
                modifier = Modifier.padding(innerPadding),
                scaffoldState = scaffoldState,
                sheetPeekHeight = 0.dp,
                sheetShape = RoundedCornerShape(0.dp),
                sheetShadowElevation = 10.dp,
                sheetDragHandle = null,
                sheetSwipeEnabled = false
            ) { padding ->
                AnimatedVisibility(visible = (control.progress > 0f && control.progress < 1f)) {
                    LinearProgressIndicator(
                        progress = { control.progress },
                        modifier = Modifier.fillMaxWidth(),
                        color = colorResource(R.color.theme_orange),
                        trackColor = colorResource(R.color.white)
                    )
                }
                WebView(
                    url = url,
                    control = control,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    onReceivedTitle = {
                        title = it
                        scope.launch {
                            WanHelper.setBrowseHistory(it.toString(), url)
                        }
                    },
                    onCustomView = { customView = it },
                    shouldOverrideUrl = shouldOverrideUrl,
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