package com.example.fragment.project.ui.web

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.fragment.project.R
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebScreen(
    url: String,
    webBookmarkData: List<String>,
    onWebBookmark: (isAdd: Boolean, text: String) -> Unit = { _, _ -> },
    onWebHistory: (isAdd: Boolean, text: String) -> Unit = { _, _ -> },
    onNavigateToBookmarkHistory: () -> Unit = {},
    onNavigateUp: () -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
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
    BottomSheetScaffold(
        sheetContent = {
            Column {
                Row(
                    modifier = Modifier
                        .background(colorResource(R.color.white))
                        .height(50.dp)
                ) {
                    Button(
                        onClick = {
                            navigator.navigateBack()
                        },
                        shape = RoundedCornerShape(0),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colorResource(R.color.white),
                            contentColor = colorResource(R.color.theme)
                        ),
                        elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp),
                        contentPadding = PaddingValues(17.dp),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        Icon(
                            painter = painterResource(R.mipmap.ic_web_back),
                            contentDescription = null,
                            tint = colorResource(R.color.theme)
                        )
                    }
                    Button(
                        onClick = {
                            navigator.navigateForward()
                        },
                        shape = RoundedCornerShape(0),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colorResource(R.color.white),
                            contentColor = colorResource(R.color.theme)
                        ),
                        elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp),
                        contentPadding = PaddingValues(17.dp),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        Icon(
                            painter = painterResource(R.mipmap.ic_web_forward),
                            contentDescription = null,
                            tint = colorResource(R.color.theme)
                        )
                    }
                    Button(
                        onClick = {
                            navigator.reload()
                            scope.launch { bottomSheetState.partialExpand() }
                        },
                        shape = RoundedCornerShape(0),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colorResource(R.color.white),
                            contentColor = colorResource(R.color.theme)
                        ),
                        elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp),
                        contentPadding = PaddingValues(15.dp),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        Icon(
                            painter = painterResource(R.mipmap.ic_web_refresh),
                            contentDescription = null,
                            tint = colorResource(R.color.theme)
                        )
                    }
                    Button(
                        onClick = {
                            scope.launch {
                                if (sheetValue == SheetValue.PartiallyExpanded) {
                                    bottomSheetState.expand()
                                } else {
                                    bottomSheetState.partialExpand()
                                }
                            }
                        },
                        shape = RoundedCornerShape(0),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colorResource(R.color.white),
                            contentColor = colorResource(R.color.theme)
                        ),
                        elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp),
                        contentPadding = PaddingValues(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        Icon(
                            painter = painterResource(R.mipmap.ic_web_more),
                            contentDescription = null,
                            tint = colorResource(R.color.theme)
                        )
                    }
                }
                Row(
                    modifier = Modifier
                        .background(colorResource(R.color.white))
                        .height(50.dp)
                ) {
                    Button(
                        onClick = {
                            try {
                                val uri = Uri.parse(navigator.lastLoadedUrl)
                                val intent = Intent(Intent.ACTION_VIEW, uri)
                                intent.addCategory(Intent.CATEGORY_BROWSABLE)
                                context.startActivity(intent)
                                scope.launch { bottomSheetState.partialExpand() }
                            } catch (e: Exception) {
                                Log.e(this.javaClass.name, e.message.toString())
                            }
                        },
                        shape = RoundedCornerShape(0),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colorResource(R.color.white),
                            contentColor = colorResource(R.color.theme)
                        ),
                        elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp),
                        contentPadding = PaddingValues(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        Icon(
                            painter = painterResource(R.mipmap.ic_web_browse),
                            contentDescription = null,
                            tint = colorResource(R.color.theme)
                        )
                    }
                    Button(
                        onClick = {
                            onNavigateToBookmarkHistory()
                            scope.launch { bottomSheetState.partialExpand() }
                        },
                        shape = RoundedCornerShape(0),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colorResource(R.color.white),
                            contentColor = colorResource(R.color.theme)
                        ),
                        elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp),
                        contentPadding = PaddingValues(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        Icon(
                            painter = painterResource(R.mipmap.ic_web_history),
                            contentDescription = null,
                            tint = colorResource(R.color.theme)
                        )
                    }
                    Button(
                        onClick = {
                            onWebBookmark(
                                !webBookmarkData.contains(navigator.lastLoadedUrl),
                                navigator.lastLoadedUrl.toString()
                            )
                        },
                        shape = RoundedCornerShape(0),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colorResource(R.color.white),
                            contentColor = colorResource(R.color.theme)
                        ),
                        elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp),
                        contentPadding = PaddingValues(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        Icon(
                            painter = painterResource(R.mipmap.ic_web_bookmark),
                            contentDescription = null,
                            tint = colorResource(
                                if (webBookmarkData.contains(navigator.lastLoadedUrl)) {
                                    R.color.theme_orange
                                } else {
                                    R.color.theme
                                }
                            )
                        )
                    }
                    Button(
                        onClick = {
                            navigator.injectVConsole()
                            scope.launch { bottomSheetState.partialExpand() }
                        },
                        shape = RoundedCornerShape(0),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colorResource(R.color.white),
                            contentColor = colorResource(R.color.theme)
                        ),
                        elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp),
                        contentPadding = PaddingValues(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        Icon(
                            painter = painterResource(R.mipmap.ic_web_debug),
                            contentDescription = null,
                            tint = colorResource(
                                if (navigator.injectVConsole) {
                                    R.color.theme_orange
                                } else {
                                    R.color.theme
                                }
                            )
                        )
                    }
                }
            }
        },
        scaffoldState = scaffoldState,
        sheetPeekHeight = 50.dp,
        sheetShape = RoundedCornerShape(0.dp),
        sheetShadowElevation = 10.dp,
        sheetDragHandle = null,
        sheetSwipeEnabled = false
    ) { padding ->
        WebViewNavGraph(
            url = url,
            navigator = navigator,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            onLoadUrl = { url ->
                onWebHistory(true, url)
            },
            onNavigateUp = onNavigateUp
        )
        AnimatedVisibility(visible = (navigator.progress > 0f && navigator.progress < 1f)) {
            LinearProgressIndicator(
                progress = { navigator.progress },
                modifier = Modifier.fillMaxWidth(),
                color = colorResource(R.color.theme_orange),
                trackColor = colorResource(R.color.white)
            )
        }
    }
}