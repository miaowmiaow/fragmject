package com.example.fragmject.feature.auth.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fragmject.core.ui.components.BackIcon
import com.example.fragmject.feature.auth.impl.R

/**
 * 公共账号表单：统一 LoginScreen / RegisterScreen 重复的
 * 「背景图 + 返回按钮 + 标题 + 输入框 + 提交按钮 + 底部跳转链接」骨架。
 *
 * loading 期间用半透明遮罩 + [CircularProgressIndicator] 覆盖整个表单，
 * 替代原先的骨架屏，并拦截重复点击。
 */
@Composable
fun AccountForm(
    texts: AccountFormTexts,
    onBack: () -> Unit,
    onSubmit: () -> Unit,
    onBottomLinkClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    fields: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .paint(
                painter = painterResource(id = R.mipmap.bg),
                contentScale = ContentScale.FillBounds
            )
            .fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .verticalScroll(rememberScrollState())
        ) {
            BackIcon(onBack)
            Spacer(Modifier.height(30.dp))
            AccountFormHeader(title = texts.title, subtitle = texts.subtitle)
            Spacer(Modifier.weight(1f))
            fields()
            Spacer(Modifier.height(30.dp))
            AccountFormSubmitRow(buttonText = texts.buttonText, onSubmit = onSubmit)
            Spacer(Modifier.height(30.dp))
            AccountFormBottomLink(text = texts.bottomLinkText, onClick = onBottomLinkClick)
            Spacer(Modifier.height(30.dp))
        }
        if (texts.isLoading) {
            AccountFormLoadingOverlay()
        }
    }
}

/** 标题 + 副标题。 */
@Composable
private fun AccountFormHeader(title: String, subtitle: String) {
    Text(
        text = title,
        modifier = Modifier.padding(horizontal = 20.dp),
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.onPrimaryContainer,
    )
    Spacer(Modifier.height(20.dp))
    Text(
        text = subtitle,
        modifier = Modifier.padding(horizontal = 20.dp),
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onPrimaryContainer,
    )
}

/** 左侧按钮文案 + 右侧圆形提交按钮。 */
@Composable
private fun AccountFormSubmitRow(buttonText: String, onSubmit: () -> Unit) {
    Row(
        modifier = Modifier.padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = buttonText,
            fontSize = 20.sp,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Spacer(Modifier.weight(1f))
        Button(
            onClick = onSubmit,
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.secondaryContainer
            ),
            elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp),
            contentPadding = PaddingValues(15.dp),
            modifier = Modifier.size(55.dp)
        ) {
            Icon(
                painter = painterResource(R.mipmap.ic_right_arrow),
                contentDescription = null
            )
        }
    }
}

/** 底部「去登录 / 去注册」链接。 */
@Composable
private fun AccountFormBottomLink(text: String, onClick: () -> Unit) {
    Text(
        text = text,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 25.dp),
        textDecoration = TextDecoration.Underline,
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.onPrimaryContainer,
    )
}

/** loading 遮罩：半透明黑底 + 居中进度圈，同时拦截重复点击。 */
@Composable
private fun AccountFormLoadingOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.3f)),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            color = MaterialTheme.colorScheme.primaryContainer,
            trackColor = MaterialTheme.colorScheme.secondary
        )
    }
}
