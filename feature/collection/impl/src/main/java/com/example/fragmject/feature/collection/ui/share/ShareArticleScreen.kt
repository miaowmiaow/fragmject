package com.example.fragmject.feature.collection.ui.share

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.fragmject.core.designsystem.AppTheme
import com.example.fragmject.core.navigation.contract.LocalNavigators
import com.example.fragmject.core.navigation.runtime.LocalOnNavigateUp
import com.example.fragmject.core.ui.components.ClearTextField
import com.example.fragmject.core.ui.components.LoadingContent
import com.example.fragmject.core.ui.components.TitleBar
import com.example.fragmject.feature.collection.impl.R
import kotlinx.coroutines.launch

@Composable
fun ShareArticleScreen(
    viewModel: ShareArticleViewModel = hiltViewModel(key = "share_article"),
) {
    // 无需复位成功标记：ViewModel 已限定到 NavEntry，出栈即清除，再次进入必为全新实例
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val articleNavigator = LocalNavigators.current.article
    val onNavigateUp = LocalOnNavigateUp.current
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val titleText = uiState.title
    val linkText = uiState.link
    LaunchedEffect(uiState.message) {
        if (uiState.message.isNotBlank() && !uiState.success) {
            snackbarHostState.showSnackbar(uiState.message)
            viewModel.resetMessage()
        }
    }
    LaunchedEffect(uiState.success) {
        if (uiState.success) {
            // 分享成功：直接返回上一页（返回即成功反馈，避免 snackbar 随页面销毁丢失）
            onNavigateUp()
        }
    }
    val openLink: () -> Unit = {
        if (linkText.isBlank()) {
            scope.launch { snackbarHostState.showSnackbar("文章链接不能为空") }
        } else {
            articleNavigator.openArticle(linkText)
        }
    }
    Scaffold(
        topBar = {
            TitleBar(
                title = "分享文章",
                onNavigateUp = onNavigateUp,
                actions = { ShareOpenLinkAction(onClick = openLink) },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) { data -> Snackbar(snackbarData = data) } },
    ) { innerPadding ->
        LoadingContent(isLoading = uiState.isLoading) {
            Box(modifier = Modifier.padding(innerPadding)) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 15.dp)
                        .verticalScroll(scrollState),
                ) {
                    ShareInputField(
                        label = "文章标题",
                        placeholder = "请输入文章标题",
                        value = titleText,
                        onValueChange = viewModel::updateTitle,
                        onClear = { viewModel.updateTitle("") },
                    )
                    Spacer(Modifier.height(15.dp))
                    ShareInputField(
                        label = "文章链接",
                        placeholder = "请输入文章链接",
                        value = linkText,
                        onValueChange = viewModel::updateLink,
                        onClear = { viewModel.updateLink("") },
                    )
                    Spacer(Modifier.height(15.dp))
                    Text(
                        text = "记得点击右上角按钮检查链接哦",
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        fontSize = 12.sp,
                    )
                    Spacer(Modifier.height(20.dp))
                    ShareSubmitButton(isLoading = uiState.isLoading, onClick = viewModel::submit)
                    Spacer(Modifier.weight(1f))
                    ShareArticleNotice()
                }
            }
        }
    }
}

/** 标题/链接共用的输入行：标签 + 可清除输入框。 */
@Composable
private fun ShareInputField(
    label: String,
    placeholder: String,
    value: String,
    onValueChange: (String) -> Unit,
    onClear: () -> Unit,
) {
    Text(
        text = label,
        fontSize = 14.sp,
    )
    Spacer(Modifier.height(15.dp))
    ClearTextField(
        value = value,
        onValueChange = onValueChange,
        onClear = onClear,
        modifier = Modifier.height(45.dp),
        textStyle = TextStyle.Default.copy(
            fontSize = 14.sp,
            background = Color.Transparent,
        ),
        placeholder = {
            Text(
                text = placeholder,
                fontSize = 14.sp,
            )
        },
        keyboardOptions = KeyboardOptions.Default.copy(
            imeAction = ImeAction.Next
        ),
        colors = TextFieldDefaults.colors(
            focusedIndicatorColor = MaterialTheme.colorScheme.primaryContainer,
            unfocusedIndicatorColor = MaterialTheme.colorScheme.primaryContainer,
        ),
    )
}

/** 右上角「预览链接」按钮。 */
@Composable
private fun ShareOpenLinkAction(onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(
            painter = painterResource(R.mipmap.ic_browser),
            contentDescription = null,
            modifier = Modifier.padding(8.dp),
            tint = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

/** 底部提交按钮；`align` 需要 ColumnScope 接收者。 */
@Composable
private fun ColumnScope.ShareSubmitButton(isLoading: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = !isLoading,
        modifier = Modifier
            .fillMaxWidth(0.8f)
            .align(Alignment.CenterHorizontally),
        shape = RoundedCornerShape(5.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primaryContainer),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        contentPadding = PaddingValues(0.dp, 15.dp, 0.dp, 15.dp)
    ) {
        Text(
            text = if (isLoading) "分享中…" else "分享",
            fontSize = 16.sp
        )
    }
}

/** 底部投稿说明。 */
@Composable
private fun ShareArticleNotice() {
    Text(
        text = "1. 只要是任何好文都可以分享哈，并不一定要是原创！投递的文章会进入广场 tab;",
        fontSize = 12.sp,
    )
    Text(
        text = "2. CSDN，掘金，简书等官方博客站点会直接通过，不需要审核;",
        fontSize = 12.sp,
    )
    Text(
        text = "3. 其他个人站点会进入审核阶段，不要投递任何无效链接，否则可能会对你的账号产生一定影响;",
        fontSize = 12.sp,
    )
    Text(
        text = "4. 如果你发现错误，可以提交日志，让我们一起使网站变得更好。",
        fontSize = 12.sp,
    )
    Text(
        text = "5. 由于本站为个人开发与维护，会尽力保证24小时内审核，当然有可能哪天太累，会延期，请保持佛系...",
        fontSize = 12.sp,
    )
}

@Preview(showBackground = true, backgroundColor = 0xFFF0F0F0)
@Composable
fun ShareArticleScreenPreview() {
    AppTheme { ShareArticleScreen() }
}
