package com.example.fragmject.feature.picture.ui.editor

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.createBitmap
import com.example.fragmject.feature.picture.model.StickerAttrs
import com.example.fragmject.feature.picture.utils.ColorUtils

@Composable
fun PictureTextSheet(
    attrs: StickerAttrs? = null,
    onFinish: (StickerAttrs) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf(attrs?.description ?: "") }
    var selectedColorIndex by remember { mutableIntStateOf(0) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .windowInsetsPadding(WindowInsets.systemBars)
            .imePadding()
            .padding(16.dp)
    ) {
        // 顶部栏
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onDismiss) {
                Text("返回", color = Color.White, fontSize = 16.sp)
            }
            TextButton(
                onClick = {
                    val paint = Paint().apply {
                        isAntiAlias = true
                        color = ColorUtils.colorful[selectedColorIndex]
                        textSize = 72f
                        typeface = Typeface.DEFAULT_BOLD
                    }
                    val textWidth = paint.measureText(text)
                    val fm = paint.fontMetrics
                    val textHeight = fm.descent - fm.ascent
                    val padding = 24f
                    val bmp = createBitmap(
                        (textWidth + padding * 2).toInt().coerceAtLeast(1),
                        (textHeight + padding * 2).toInt().coerceAtLeast(1)
                    )
                    val canvas = Canvas(bmp)
                    canvas.drawText(text, padding, -fm.ascent + padding, paint)
                    val stickerAttrs = StickerAttrs(bmp, description = text)
                    onFinish(stickerAttrs)
                },
                enabled = text.isNotBlank()
            ) {
                Text("完成", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // 文本输入区
        BasicTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .focusRequester(focusRequester)
                .background(Color.Transparent),
            textStyle = TextStyle(
                color = Color(ColorUtils.colorful[selectedColorIndex]),
                fontSize = 24.sp,
                fontWeight = FontWeight.Medium
            ),
            decorationBox = { innerTextField ->
                Box {
                    if (text.isEmpty()) {
                        Text(
                            "请输入文字...",
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 24.sp
                        )
                    }
                    innerTextField()
                }
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 颜色选择器
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ColorUtils.colorful.forEachIndexed { index, colorInt ->
                val isSelected = index == selectedColorIndex
                Box(
                    modifier = Modifier
                        .padding(horizontal = 6.dp)
                        .size(if (isSelected) 40.dp else 32.dp)
                        .clip(CircleShape)
                        .background(Color(colorInt))
                        .border(
                            if (isSelected) 3.dp else 0.dp,
                            Color.White,
                            CircleShape
                        )
                        .clickable { selectedColorIndex = index }
                )
            }
        }

        Spacer(modifier = Modifier.height(48.dp))
    }
}