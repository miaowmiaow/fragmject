package com.example.fragment.project.ui.web

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.fragment.project.R
import com.example.fragment.project.ui.web.content.WebViewManager
import com.example.fragment.project.ui.web.content.WebViewNavGraph
import com.example.fragment.project.ui.web.content.rememberWebViewNavigator
import kotlinx.coroutines.launch

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun WebScreen(
    originalUrl: String,
    webCollectList: List<String>,
    onWebBrowseHistory: (isAdd: Boolean, text: String) -> Unit = { _, _ -> },
    onWebCollect: (isAdd: Boolean, text: String) -> Unit = { _, _ -> },
    onNavigateUp: () -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
    )
    val wvNavigator = rememberWebViewNavigator()
    DisposableEffect(Unit) {
        onDispose {
            WebViewManager.reset()
        }
    }
    Column(
        modifier = Modifier
            .background(colorResource(R.color.white))
            .fillMaxSize()
            .systemBarsPadding()
    ) {
        ModalBottomSheetLayout(
            sheetState = sheetState,
            modifier = Modifier.weight(1f),
            sheetContent = {
                Row(
                    modifier = Modifier
                        .background(colorResource(R.color.white))
                        .height(50.dp)
                ) {
                    Button(
                        onClick = {
                            context.startActivity(Intent.createChooser(Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, wvNavigator.lastLoadedUrl)
                                type = "text/plain"
                            }, null))
                        },
                        elevation = ButtonDefaults.elevation(0.dp, 0.dp, 0.dp),
                        shape = RoundedCornerShape(0),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = colorResource(R.color.white),
                            contentColor = colorResource(R.color.theme)
                        ),
                        contentPadding = PaddingValues(15.dp),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        Icon(
                            painter = painterResource(R.mipmap.ic_web_share),
                            contentDescription = null,
                            tint = colorResource(R.color.theme)
                        )
                    }
                    Button(
                        onClick = {
                            try {
                                val intent =
                                    Intent(Intent.ACTION_VIEW, Uri.parse(wvNavigator.lastLoadedUrl))
                                intent.addCategory(Intent.CATEGORY_BROWSABLE)
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        },
                        elevation = ButtonDefaults.elevation(0.dp, 0.dp, 0.dp),
                        shape = RoundedCornerShape(0),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = colorResource(R.color.white),
                            contentColor = colorResource(R.color.theme)
                        ),
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
                            onWebCollect(
                                !webCollectList.contains(wvNavigator.lastLoadedUrl),
                                wvNavigator.lastLoadedUrl.toString()
                            )
                        },
                        elevation = ButtonDefaults.elevation(0.dp, 0.dp, 0.dp),
                        shape = RoundedCornerShape(0),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = colorResource(R.color.white),
                            contentColor = colorResource(R.color.theme)
                        ),
                        contentPadding = PaddingValues(15.dp),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        Icon(
                            painter = painterResource(
                                if (webCollectList.contains(wvNavigator.lastLoadedUrl)) {
                                    R.mipmap.ic_collect_checked
                                } else {
                                    R.mipmap.ic_collect_unchecked
                                }
                            ),
                            contentDescription = null,
                            tint = colorResource(
                                if (webCollectList.contains(wvNavigator.lastLoadedUrl)) {
                                    R.color.pink
                                } else {
                                    R.color.theme
                                }
                            )
                        )
                    }
                    Button(
                        onClick = {
                            wvNavigator.injectVConsole()
                            scope.launch { sheetState.hide() }
                        },
                        elevation = ButtonDefaults.elevation(0.dp, 0.dp, 0.dp),
                        shape = RoundedCornerShape(0),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = colorResource(R.color.white),
                            contentColor = colorResource(R.color.theme)
                        ),
                        contentPadding = PaddingValues(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        Icon(
                            painter = painterResource(R.mipmap.ic_web_debug),
                            contentDescription = null,
                            tint = colorResource(
                                if (wvNavigator.injectVConsole) {
                                    R.color.theme_orange
                                } else {
                                    R.color.theme
                                }
                            )
                        )
                    }
                }
            }
        ) {
            WebViewNavGraph(
                originalUrl = originalUrl,
                modifier = Modifier.fillMaxSize(),
                webViewNavigator = wvNavigator,
                onWebBrowseHistory = onWebBrowseHistory,
                onNavigateUp = onNavigateUp
            )
        }
        AnimatedVisibility(visible = (wvNavigator.progress > 0f && wvNavigator.progress < 1f)) {
            LinearProgressIndicator(
                progress = wvNavigator.progress,
                modifier = Modifier.fillMaxWidth(),
                color = colorResource(R.color.theme_orange),
                backgroundColor = colorResource(R.color.white)
            )
        }
        Row(
            modifier = Modifier
                .background(colorResource(R.color.white))
                .height(50.dp)
        ) {
            Button(
                onClick = {
                    wvNavigator.navigateBack()
                },
                elevation = ButtonDefaults.elevation(0.dp, 0.dp, 0.dp),
                shape = RoundedCornerShape(0),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = colorResource(R.color.white),
                    contentColor = colorResource(R.color.theme)
                ),
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
                    wvNavigator.navigateForward()
                },
                elevation = ButtonDefaults.elevation(0.dp, 0.dp, 0.dp),
                shape = RoundedCornerShape(0),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = colorResource(R.color.white),
                    contentColor = colorResource(R.color.theme)
                ),
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
                    wvNavigator.reload()
                },
                elevation = ButtonDefaults.elevation(0.dp, 0.dp, 0.dp),
                shape = RoundedCornerShape(0),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = colorResource(R.color.white),
                    contentColor = colorResource(R.color.theme)
                ),
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
                        if (sheetState.isVisible) {
                            sheetState.hide()
                        } else {
                            sheetState.show()
                        }
                    }
                },
                elevation = ButtonDefaults.elevation(0.dp, 0.dp, 0.dp),
                shape = RoundedCornerShape(0),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = colorResource(R.color.white),
                    contentColor = colorResource(R.color.theme)
                ),
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
    }
}