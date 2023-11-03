package com.example.fragment.project.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.example.fragment.project.R

@Composable
fun EllipsisText(
    text: AnnotatedString,
    color: Color,
    backgroundColor: Color,
    fontSize: TextUnit = TextUnit.Unspecified,
    lineHeight: TextUnit = TextUnit.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
    inlineContent: Map<String, InlineTextContent> = mapOf(),
    onTextLayout: (TextLayoutResult) -> Unit = {},
    ellipsisText: String = "...全文",
    ellipsisColor: Color = colorResource(R.color.blue),
    onClick: () -> Unit = {},
    onEllipsisClick: () -> Unit = {},
) {
    val style = TextStyle.Default.copy(color = color, fontSize = fontSize)
    var ellipsisBottom by remember { mutableFloatStateOf(0f) }
    var ellipsisRight by remember { mutableFloatStateOf(0f) }
    var ellipsisWidth by remember { mutableStateOf(0.dp) }
    val ellipsisMeasure = rememberTextMeasurer()
    val ellipsisLayoutResult = ellipsisMeasure.measure(
        text = ellipsisText,
        style = style
    )
    Box {
        Text(
            text = text,
            modifier = Modifier
                .clickable(
                    onClick = onClick,
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                )
                .background(backgroundColor),
            lineHeight = if (lineHeight != TextUnit.Unspecified) {
                lineHeight
            } else {
                fontSize.times(1.35f)
            },
            overflow = TextOverflow.Ellipsis,
            maxLines = maxLines,
            inlineContent = inlineContent,
            onTextLayout = {
                val offset = it.getOffsetForPosition(
                    Offset(
                        it.getLineRight(it.lineCount - 1) - ellipsisLayoutResult.size.width,
                        it.getLineTop(it.lineCount - 1)
                    )
                )
                ellipsisBottom = it.getLineBottom(it.lineCount - 1)
                ellipsisRight = it.getHorizontalPosition(offset, true)
                ellipsisWidth = Dp(it.size.width - ellipsisRight)
                onTextLayout(it)
            },
            style = style
        )
        Box(modifier = Modifier
            .graphicsLayer {
                translationX = ellipsisRight
                translationY = ellipsisBottom - size.height
            }
            .background(backgroundColor)
            .width(ellipsisWidth)
        ) {
            Text(
                text = ellipsisText,
                modifier = Modifier.clickable { onEllipsisClick() },
                style = style.copy(color = ellipsisColor)
            )
        }
    }
}
