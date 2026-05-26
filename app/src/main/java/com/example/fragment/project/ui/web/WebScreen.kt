package com.example.fragment.project.ui.web

import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.example.fragment.project.BrowseHistoryRoute
import com.example.fragment.project.R
import com.example.fragment.project.WanTheme
import com.example.fragment.project.WebRoute
import com.example.fragment.project.components.TitleBar
import com.example.fragment.project.data.History
import com.example.fragment.project.utils.WanHelper
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebScreen(
    url: String,
    onNavigate: (route: Any) -> Unit = {},
    onNavigateUp: () -> Unit = {},
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
        // 之前在外层 collect 内嵌套了第二层 collect，外层永不结束、内层永远不会执行，
        // 导致 bookmark 字段始终拿不到值。改为直接对单层流收集即可。
        WanHelper.getBookmark().collect { bookmarks ->
            bookmark = bookmarks.firstOrNull { it.url == url }
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
                                        onNavigate(BrowseHistoryRoute)
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
                val progressColor = colorResource(R.color.theme_orange)
                // 进度值平滑插值，避免 onProgressChanged 的离散跳变；完成时稍快收尾
                val animatedProgress by animateFloatAsState(
                    targetValue = control.progress,
                    animationSpec = if (control.progress >= 1f) {
                        tween(durationMillis = 180)
                    } else {
                        spring(stiffness = Spring.StiffnessLow)
                    },
                    label = "webProgress"
                )
                AnimatedVisibility(
                    visible = (control.progress > 0f && control.progress < 1f),
                    enter = fadeIn(tween(120)),
                    exit = fadeOut(tween(260))
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                    ) {
                        val trackHeight = size.height
                        val width = size.width
                        val cap = trackHeight / 2f
                        // 弱化的轨道：极淡的同色调底
                        drawLine(
                            color = progressColor.copy(alpha = 0.12f),
                            start = Offset(0f, trackHeight / 2f),
                            end = Offset(width, trackHeight / 2f),
                            strokeWidth = trackHeight,
                            cap = StrokeCap.Round
                        )
                        val progressWidth = (width * animatedProgress).coerceIn(0f, width)
                        if (progressWidth > 0f) {
                            // 主体进度
                            drawLine(
                                color = progressColor,
                                start = Offset(0f, trackHeight / 2f),
                                end = Offset(progressWidth, trackHeight / 2f),
                                strokeWidth = trackHeight,
                                cap = StrokeCap.Round
                            )
                            // 头部渐变拖尾光晕：从透明到主色，集中在头部约 24dp 区间
                            val glowWidth = 24.dp.toPx().coerceAtMost(progressWidth)
                            val glowStart = (progressWidth - glowWidth).coerceAtLeast(0f)
                            val brush = Brush.horizontalGradient(
                                colors = listOf(
                                    progressColor.copy(alpha = 0f),
                                    progressColor.copy(alpha = 0.55f)
                                ),
                                startX = glowStart,
                                endX = progressWidth
                            )
                            drawRect(
                                brush = brush,
                                topLeft = Offset(glowStart, 0f),
                                size = Size(glowWidth, trackHeight)
                            )
                            // 头部高亮点：让"前进感"更明显
                            drawCircle(
                                color = Color.White.copy(alpha = 0.9f),
                                radius = cap * 0.55f,
                                center = Offset(progressWidth - cap, trackHeight / 2f),
                                style = Stroke(width = 0f)
                            )
                        }
                    }
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
                    shouldOverrideUrl = { onNavigate(WebRoute(it)) },
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