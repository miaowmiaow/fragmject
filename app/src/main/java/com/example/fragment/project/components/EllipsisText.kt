package com.example.fragment.project.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.unit.TextUnit
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
    var ellipsisLeft by remember { mutableFloatStateOf(0f) }
    val ellipsisMeasure = rememberTextMeasurer()
    val ellipsisLayoutResult = ellipsisMeasure.measure(
        text = ellipsisText,
        style = style
    )
    val ellipsisWidth = ellipsisLayoutResult.size.width
    Box(modifier = Modifier.animateContentSize()) {
        Text(
            text = text,
            modifier = Modifier
                .clickable(
                    onClick = onClick,
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                )
                .background(backgroundColor),
            lineHeight = lineHeight,
            overflow = TextOverflow.Ellipsis,
            maxLines = maxLines,
            inlineContent = inlineContent,
            onTextLayout = {
                val offset = if (maxLines == Int.MAX_VALUE) 0 else ellipsisWidth
                ellipsisBottom = it.getLineBottom(it.lineCount - 1)
                ellipsisLeft = it.getHorizontalPosition(
                    it.getOffsetForPosition(
                        Offset(
                            it.getLineRight(it.lineCount - 1) - offset,
                            it.getLineTop(it.lineCount - 1)
                        )
                    ), true
                )
                if (ellipsisLeft + ellipsisWidth > it.size.width) {
                    ellipsisLeft = it.getHorizontalPosition(
                        it.getOffsetForPosition(
                            Offset(
                                (it.size.width - ellipsisWidth).toFloat(),
                                it.getLineTop(it.lineCount - 1)
                            )
                        ), true
                    )
                }
                onTextLayout(it)
            },
            style = style
        )
        Text(
            text = "$ellipsisText ",
            modifier = Modifier
                .graphicsLayer {
                    translationX = ellipsisLeft
                    translationY = ellipsisBottom - size.height
                }
                .clickable { onEllipsisClick() }
                .background(backgroundColor),
            style = style.copy(color = ellipsisColor)
        )
    }
}
