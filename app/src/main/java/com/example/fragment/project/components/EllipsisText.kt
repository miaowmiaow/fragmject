package com.example.fragment.project.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import com.example.fragment.project.R

@Composable
fun EllipsisText(
    text: AnnotatedString,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
    inlineContent: Map<String, InlineTextContent> = mapOf(),
    background: Color,
    ellipsisText: String = "...全文",
    ellipsisColor: Color = colorResource(R.color.blue),
    onClick: () -> Unit = {},
    onEllipsisClick: () -> Unit = {},
) {
    val style = TextStyle.Default.copy(color = color, fontSize = fontSize)
    var right by remember { mutableFloatStateOf(0f) }
    var bottom by remember { mutableFloatStateOf(0f) }
    val ellipsisMeasure = rememberTextMeasurer()
    val ellipsisLayoutResult = ellipsisMeasure.measure(
        text = ellipsisText,
        style = style
    )
    Box(modifier = Modifier.clipToBounds()) {
        Text(
            text = text,
            modifier = Modifier
                .clickable(
                    onClick = onClick,
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                )
                .background(background),
            lineHeight = fontSize.times(1.35f),
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
                right = it.getHorizontalPosition(offset, true)
                bottom = it.getLineBottom(it.lineCount - 1)
            },
            style = style
        )
        Box(modifier = Modifier
            .graphicsLayer {
                translationX = right
                translationY = bottom - size.height
            }
            .background(background)
            .fillMaxWidth()
        ) {
            Text(
                text = ellipsisText,
                modifier = Modifier.clickable { onEllipsisClick() },
                style = style.copy(color = ellipsisColor)
            )
        }
    }
}
