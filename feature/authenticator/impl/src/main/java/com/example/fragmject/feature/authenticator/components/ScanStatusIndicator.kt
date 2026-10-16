package com.example.fragmject.feature.authenticator.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fragmject.feature.authenticator.model.ScanState
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay

/** 识别成功 / 失败的居中指示器（失败 1.5 秒后自动消失）。 */
@Composable
fun ScanStatusIndicator(
    state: ScanState,
    onErrorShown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state is ScanState.Error) {
        LaunchedEffect(state) {
            delay(1500.milliseconds)
            onErrorShown()
        }
        AnimatedVisibility(
            visible = true,
            enter = scaleIn(tween(300)) + fadeIn(),
            exit = scaleOut() + fadeOut(),
            modifier = modifier,
        ) {
            StatusBubble(
                background = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                icon = {
                    Icon(
                        Icons.Default.Error,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(40.dp),
                    )
                },
                text = state.message,
            )
        }
        return
    }
    if (state is ScanState.Success) {
        AnimatedVisibility(
            visible = true,
            enter = scaleIn(tween(300)) + fadeIn(),
            exit = scaleOut() + fadeOut(),
            modifier = modifier,
        ) {
            StatusBubble(
                background = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary,
                icon = {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondary,
                        modifier = Modifier.size(40.dp),
                    )
                },
                text = "识别成功",
            )
        }
        return
    }
    if (state is ScanState.Detected) {
        Column(
            modifier = modifier,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primaryContainer,
                trackColor = MaterialTheme.colorScheme.secondary
            )
            Spacer(Modifier.height(12.dp))
            Text("正在处理…", color = MaterialTheme.colorScheme.onBackground, fontSize = 14.sp)
        }
    }
}

@Composable
private fun StatusBubble(
    background: Color,
    contentColor: Color,
    icon: @Composable () -> Unit,
    text: String,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(background, CircleShape),
            contentAlignment = Alignment.Center,
        ) { icon() }
        Spacer(Modifier.height(12.dp))
        Text(text = text, color = contentColor, fontSize = 15.sp, textAlign = TextAlign.Center)
    }
}
