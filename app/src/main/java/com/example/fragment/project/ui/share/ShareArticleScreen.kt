package com.example.fragment.project.ui.share

import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fragment.project.R
import com.example.fragment.project.WanTheme
import com.example.fragment.project.components.ClearTextField
import com.example.fragment.project.components.LoadingContent
import com.example.fragment.project.components.TitleBar
import kotlinx.coroutines.launch

@Composable
fun ShareArticleScreen(
    viewModel: ShareArticleViewModel = viewModel(),
    onNavigateToWeb: (url: String) -> Unit = {},
    onNavigateUp: () -> Unit = {},
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var titleText by rememberSaveable { mutableStateOf("") }
    var linkText by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(uiState.message) {
        if (uiState.message.isNotBlank()) {
            if (context is AppCompatActivity) {
                snackbarHostState.showSnackbar(uiState.message)
                viewModel.resetMessage()
            }
        }
    }
    Scaffold(
        topBar = {
            TitleBar(title = "分享文章", navigationIcon = {
                IconButton(
                    modifier = Modifier.height(45.dp),
                    onClick = onNavigateUp
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        tint = colorResource(R.color.white)
                    )
                }
            }, actions = {
                IconButton(
                    onClick = {
                        if (linkText.isBlank()) {
                            scope.launch {
                                snackbarHostState.showSnackbar("文章链接不能为空")
                            }
                            return@IconButton
                        }
                        onNavigateToWeb(linkText)
                    }
                ) {
                    Icon(
                        painter = painterResource(R.mipmap.ic_browser),
                        contentDescription = null,
                        modifier = Modifier.padding(8.dp),
                        tint = colorResource(R.color.white)
                    )
                }
            })
        },
        snackbarHost = { SnackbarHost(snackbarHostState) { data -> Snackbar(snackbarData = data) } },
    ) { innerPadding ->
        LoadingContent(uiState.isLoading) {
            Box(
                modifier = Modifier
                    .background(colorResource(R.color.white))
                    .padding(innerPadding),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(15.dp)
                        .verticalScroll(scrollState),
                ) {
                    Text(
                        text = "文章标题",
                        fontSize = 14.sp,
                        color = colorResource(R.color.text_666),
                        modifier = Modifier
                    )
                    Spacer(Modifier.height(15.dp))
                    ClearTextField(
                        value = titleText,
                        onValueChange = { titleText = it },
                        onClear = { titleText = "" },
                        modifier = Modifier.height(45.dp),
                        textStyle = TextStyle.Default.copy(
                            color = colorResource(R.color.text_333),
                            fontSize = 14.sp,
                            background = colorResource(R.color.transparent),
                        ),
                        placeholder = {
                            Text(
                                text = "请输入文章标题",
                                color = colorResource(R.color.text_ccc),
                                fontSize = 14.sp,
                            )
                        },
                        keyboardOptions = KeyboardOptions.Default.copy(
                            imeAction = ImeAction.Next
                        ),
                        colors = TextFieldDefaults.colors(
                            focusedIndicatorColor = colorResource(id = R.color.theme),
                            unfocusedIndicatorColor = colorResource(id = R.color.theme),
                        ),
                    )
                    Spacer(Modifier.height(15.dp))
                    Text(
                        text = "文章链接",
                        fontSize = 14.sp,
                        color = colorResource(R.color.text_666),
                        modifier = Modifier
                    )
                    Spacer(Modifier.height(15.dp))
                    ClearTextField(
                        value = linkText,
                        onValueChange = { linkText = it },
                        onClear = { linkText = "" },
                        modifier = Modifier.height(45.dp),
                        textStyle = TextStyle.Default.copy(
                            color = colorResource(R.color.text_333),
                            fontSize = 14.sp,
                            background = colorResource(R.color.transparent),
                        ),
                        placeholder = {
                            Text(
                                text = "请输入文章链接",
                                color = colorResource(R.color.text_ccc),
                                fontSize = 14.sp,
                            )
                        },
                        keyboardOptions = KeyboardOptions.Default.copy(
                            imeAction = ImeAction.Next
                        ),
                        colors = TextFieldDefaults.colors(
                            focusedIndicatorColor = colorResource(id = R.color.theme),
                            unfocusedIndicatorColor = colorResource(id = R.color.theme),
                        ),
                    )
                    Spacer(Modifier.height(15.dp))
                    Text(
                        text = "记得点击右上角按钮检查链接哦",
                        color = colorResource(R.color.theme_orange),
                        fontSize = 12.sp,
                    )
                    Spacer(Modifier.height(20.dp))
                    Button(
                        onClick = {
                            if (titleText.isBlank()) {
                                scope.launch {
                                    snackbarHostState.showSnackbar("文章标题不能为空")
                                }
                                return@Button
                            }
                            if (linkText.isBlank()) {
                                scope.launch {
                                    snackbarHostState.showSnackbar("文章链接不能为空")
                                }
                                return@Button
                            }
                            viewModel.share(titleText, linkText)
                        },
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .align(Alignment.CenterHorizontally),
                        shape = RoundedCornerShape(5.dp),
                        border = BorderStroke(1.dp, colorResource(R.color.theme)),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colorResource(R.color.theme),
                            contentColor = colorResource(R.color.white)
                        ),
                        contentPadding = PaddingValues(0.dp, 15.dp, 0.dp, 15.dp)
                    ) {
                        Text(
                            text = "分享",
                            fontSize = 16.sp
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = "1. 只要是任何好文都可以分享哈，并不一定要是原创！投递的文章会进入广场 tab;",
                        color = colorResource(R.color.text_999),
                        fontSize = 12.sp,
                    )
                    Text(
                        text = "2. CSDN，掘金，简书等官方博客站点会直接通过，不需要审核;",
                        color = colorResource(R.color.text_999),
                        fontSize = 12.sp,
                    )
                    Text(
                        text = "3. 其他个人站点会进入审核阶段，不要投递任何无效链接，否则可能会对你的账号产生一定影响;",
                        color = colorResource(R.color.text_999),
                        fontSize = 12.sp,
                    )
                    Text(
                        text = "4. 如果你发现错误，可以提交日志，让我们一起使网站变得更好。",
                        color = colorResource(R.color.text_999),
                        fontSize = 12.sp,
                    )
                    Text(
                        text = "5. 由于本站为个人开发与维护，会尽力保证24小时内审核，当然有可能哪天太累，会延期，请保持佛系...",
                        color = colorResource(R.color.text_999),
                        fontSize = 12.sp,
                    )
                }
            }
        }
    }

}

@Preview(showBackground = true, backgroundColor = 0xFFF0F0F0)
@Composable
fun ShareArticleScreenPreview() {
    WanTheme { ShareArticleScreen() }
}