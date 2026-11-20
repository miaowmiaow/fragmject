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
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.example.fragmject.core.designsystem.AppColors
import com.example.fragmject.core.image.editor.model.StickerAttrs
import com.example.fragmject.feature.picture.utils.ColorUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun PictureTextSheet(
    attrs: StickerAttrs? = null,
    onFinish: (StickerAttrs) -> Unit,
    onDismiss: () -> Unit,
) {
    val textFieldState = rememberTextFieldState(
        initialText = attrs?.description.orEmpty().takeCodePoints(MAX_TEXT_CODE_POINTS),
    )
    val text = textFieldState.text.toString()
    var selectedColorIndex by remember { mutableIntStateOf(0) }
    var isBuildingSticker by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.editorBackground)
            .windowInsetsPadding(WindowInsets.systemBars)
            .imePadding()
            .padding(16.dp)
    ) {
        TextSheetTopBar(
            canConfirm = text.isNotBlank(),
            isBuilding = isBuildingSticker,
            onDismiss = onDismiss,
            onConfirm = {
                if (!isBuildingSticker) {
                    val colorSnapshot = selectedColorIndex
                    isBuildingSticker = true
                    scope.launch {
                        try {
                            val sticker = withContext(Dispatchers.Default) {
                                buildTextSticker(text, colorSnapshot)
                            }
                            onFinish(sticker)
                        } finally {
                            isBuildingSticker = false
                        }
                    }
                }
            },
        )
        Spacer(modifier = Modifier.height(32.dp))
        TextInputField(
            state = textFieldState,
            colorIndex = selectedColorIndex,
            focusRequester = focusRequester,
            enabled = !isBuildingSticker,
        )
        Text(
            text = "${text.codePointCount(0, text.length)}/$MAX_TEXT_CODE_POINTS",
            modifier = Modifier.align(Alignment.End),
            color = AppColors.editorOnBackground.copy(alpha = 0.6f),
            fontSize = 12.sp,
        )
        Spacer(modifier = Modifier.height(24.dp))
        ColorPickerRow(
            selectedIndex = selectedColorIndex,
            onSelect = { selectedColorIndex = it },
        )
        Spacer(modifier = Modifier.height(48.dp))
    }
}

/** 顶部栏：返回 / 完成（无文字时不可点）。 */
@Composable
private fun TextSheetTopBar(
    canConfirm: Boolean,
    isBuilding: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(onClick = onDismiss, enabled = !isBuilding) {
            Text("返回", color = AppColors.editorOnBackground, fontSize = 16.sp)
        }
        TextButton(
            onClick = onConfirm,
            enabled = canConfirm && !isBuilding
        ) {
            Text(
                if (isBuilding) "处理中..." else "完成",
                color = AppColors.editorOnBackground,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

/** 文字输入区，占满剩余高度，带占位提示。 */
@Composable
private fun ColumnScope.TextInputField(
    state: TextFieldState,
    colorIndex: Int,
    focusRequester: FocusRequester,
    enabled: Boolean,
) {
    BasicTextField(
        state = state,
        inputTransformation = TextCodePointLimitTransformation,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .focusRequester(focusRequester)
            .background(Color.Transparent),
        textStyle = TextStyle(
            color = Color(ColorUtils.colorful[colorIndex]),
            fontSize = 24.sp,
            fontWeight = FontWeight.Medium
        ),
        decorator = { innerTextField ->
            Box {
                if (state.text.isEmpty()) {
                    Text(
                        "请输入文字...",
                        color = AppColors.editorOnBackground.copy(alpha = 0.4f),
                        fontSize = 24.sp
                    )
                }
                innerTextField()
            }
        }
    )
}

/** 颜色选择器：选中项放大并描边。 */
@Composable
private fun ColorPickerRow(
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ColorUtils.colorful.forEachIndexed { index, colorInt ->
            val isSelected = index == selectedIndex
            Box(
                modifier = Modifier
                    .padding(horizontal = 6.dp)
                    .size(if (isSelected) 40.dp else 32.dp)
                    .clip(CircleShape)
                    .background(Color(colorInt))
                    .border(
                        if (isSelected) 3.dp else 0.dp,
                        AppColors.editorOnBackground,
                        CircleShape
                    )
                    .clickable { onSelect(index) }
            )
        }
    }
}

/** 把输入文字按当前颜色渲染成透明底 bitmap 贴图。 */
private const val MAX_TEXT_CODE_POINTS = 80

private val TextCodePointLimitTransformation = InputTransformation {
    val currentText = asCharSequence().toString()
    val limitedText = currentText.takeCodePoints(MAX_TEXT_CODE_POINTS)
    if (limitedText.length != length) {
        replace(0, length, limitedText)
    }
}

private fun String.takeCodePoints(maxCodePoints: Int): String {
    val count = codePointCount(0, length)
    return if (count <= maxCodePoints) this else substring(0, offsetByCodePoints(0, maxCodePoints))
}

private fun buildTextSticker(text: String, colorIndex: Int): StickerAttrs {
    val paint = Paint().apply {
        isAntiAlias = true
        color = ColorUtils.colorful[colorIndex]
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
    return StickerAttrs(bmp, description = text)
}
