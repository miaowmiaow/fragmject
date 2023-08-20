package com.example.fragment.project.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.example.fragment.project.R

@OptIn(ExperimentalTextApi::class)
@Composable
fun FullText(
    text: AnnotatedString,
    inlineContent: Map<String, InlineTextContent> = mapOf(),
    background: Color,
    maxLines: Int = Int.MAX_VALUE,
    style: TextStyle = LocalTextStyle.current,
    ellipsisText: String = "...全文",
    ellipsisColor: Color = colorResource(R.color.blue),
    onClick: () -> Unit = {},
    onEllipsisClick: () -> Unit = {},
) {
    var maxWidth by remember { mutableStateOf(0) }
    var right by remember { mutableStateOf(0f) }
    var bottom by remember { mutableStateOf(0f) }
    var ellipsis by remember { mutableStateOf(false) }
    val newText by remember { mutableStateOf(text) }
    val ellipsisMeasure = rememberTextMeasurer()
    val fullMeasure = rememberTextMeasurer()
    val fullLayoutResult = fullMeasure.measure(
        text = ellipsisText,
        style = style.copy(color = ellipsisColor, background = background)
    )
    Box(modifier = Modifier.clipToBounds()) {
        Text(
            text = newText,
            modifier = Modifier
                .background(background)
                .clickable(
                    onClick = {
                        onClick()
                    },
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ),
            inlineContent = inlineContent,
            lineHeight = 18.sp,
            overflow = TextOverflow.Ellipsis,
            maxLines = maxLines,
            onTextLayout = {
                maxWidth = it.size.width
                ellipsis = it.isLineEllipsized(it.lineCount - 1)
                val endOffset = it.getLineEnd(it.lineCount - 1, true)
                val offset = it.getOffsetForPosition(
                    Offset(
                        it.getLineRight(it.lineCount - 1) - fullLayoutResult.size.width,
                        it.getLineTop(it.lineCount - 1)
                    )
                )
                right = it.getHorizontalPosition(if (ellipsis) offset else endOffset, true)
                bottom = it.getLineBottom(it.lineCount - 1)
            },
            style = style
        )
        if (ellipsis) {
            Text(
                text = ellipsisText,
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        translationX = right
                        translationY = bottom - size.height
                    }
                    .background(background)
                    .clickable {
                        onEllipsisClick()
                    },
                style = style.copy(color = ellipsisColor)
            )
        }
    }
}
