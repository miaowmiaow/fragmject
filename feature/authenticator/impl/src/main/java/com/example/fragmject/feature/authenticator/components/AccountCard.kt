package com.example.fragmject.feature.authenticator.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.example.fragmject.feature.authenticator.model.OtpCodeItem

/**
 * 账户卡片（参考 HugeRivers/Authenticator）：
 * 左侧品牌色圆形头像 + 倒计时环，右侧三三分组的大号验证码。
 *
 * 交互：点按复制验证码；长按查看账户详情。
 */
@Composable
fun AccountCard(
    item: OtpCodeItem,
    onCopy: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = tween(150),
        label = "scale",
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onCopy,
                onLongClick = onLongClick,
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isPressed) 8.dp else 1.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                if (item.isTimeBased) {
                    val progress = (item.remainingMillis.toFloat() / item.periodMillis).coerceIn(0f, 1f)
                    CountdownRing(progress = progress, sizeDp = 48, strokeWidth = 4f)
                }
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            color = AuthenticatorColors.brandColor(item.account.displayIssuer),
                            shape = CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = item.account.displayIssuer.firstOrNull()?.toString() ?: "?",
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }

            Spacer(Modifier.width(16.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    text = item.account.displayIssuer,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                )
                if (item.account.accountName.isNotBlank()) {
                    Text(
                        text = item.account.accountName,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                    )
                }
            }

            AnimatedContent(
                targetState = item.displayCode,
                transitionSpec = {
                    (fadeIn(tween(200)) + scaleIn(tween(200), initialScale = 0.9f)) togetherWith
                        (fadeOut(tween(150)) + scaleOut(tween(150), targetScale = 0.9f))
                },
                label = "code",
            ) { code ->
                Text(
                    text = code,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium,
                    fontSize = 26.sp,
                    lineHeight = 1.4.em,
                    letterSpacing = 2.sp,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
        }
    }
}
