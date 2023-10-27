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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.fragment.project.R
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun WebScreen(
    originalUrl: String,
    webBookmarkData: List<String>,
    onWebBookmark: (isAdd: Boolean, text: String) -> Unit = { _, _ -> },
    onWebHistory: (isAdd: Boolean, text: String) -> Unit = { _, _ -> },
    onNavigateToBookmarkHistory: () -> Unit = {},
    onNavigateUp: () -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
    )
    val navigator = rememberWebViewNavigator()
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
                            try {
                                val uri = Uri.parse(navigator.lastLoadedUrl)
                                val intent = Intent(Intent.ACTION_VIEW, uri)
                                intent.addCategory(Intent.CATEGORY_BROWSABLE)
                                context.startActivity(intent)
                                scope.launch { sheetState.hide() }
                            } catch (e: Exception) {
                                Log.e(this.javaClass.name, e.message.toString())
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
                            onNavigateToBookmarkHistory()
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
        ) {
            WebViewNavGraph(
                originalUrl = originalUrl,
                navigator = navigator,
                modifier = Modifier.fillMaxSize(),
                onLoadUrl = {
                    onWebHistory(true, it)
                },
                onNavigateUp = onNavigateUp
            )
        }
        AnimatedVisibility(visible = (navigator.progress > 0f && navigator.progress < 1f)) {
            LinearProgressIndicator(
                progress = navigator.progress,
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
                    navigator.navigateBack()
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
                    navigator.navigateForward()
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
                    navigator.reload()
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