package com.example.fragmject.core.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import com.example.fragmject.core.designsystem.AppColors

private const val ELLIPSIS_MORE = "...展开"
private const val ELLIPSIS_COLLAPSE = "...收起"

/** 末行腾出后缀宽度时的安全余量，避免「刚好差一点」导致再多一轮布局。 */
private const val WIDTH_EPSILON = 1f

/**
 * 超出 [maxLines] 时在**末行行尾**接上「...展开」，点击可展开/收起。
 *
 * **实现方式：文本替换，而不是叠加第二个 Text。**
 * 旧实现是「正文 Text + 用 graphicsLayer 把『...展开』挪到行尾覆盖」，需要反查
 * [TextLayoutResult] 坐标、常驻第二个 Text 与一个 layer，且定位依赖多轮布局收敛。
 * 现在只做一件事：把末行尾部若干字符**换成**后缀，仍是一个 Text——
 * 省掉坐标反查、第二个 Text 的 measure/layout、graphicsLayer 与遮挡用的背景。
 *
 * 截断位置怎么来：第一轮按原文本布局，[TextLayoutResult] 已给出末行可见结尾
 * [TextLayoutResult.getLineEnd]；再用 [TextLayoutResult.getHorizontalPosition] 从行尾
 * 往前累加字符宽度，直到腾出 ≥ 后缀宽度的空间。**不需要**对正文做任何 measure。
 *
 * @param cutAt 仅内部使用：-1 表示未截断；≥0 表示折叠态截断到的字符下标
 * @param animate 展开/收起时是否做尺寸动画；信息流等列表场景建议 false
 * @param onClick 正文点击回调；无论是否截断，点击正文都会先回调它，再切换展开态
 */
@Composable
fun EllipsisText(
    text: AnnotatedString,
    color: Color,
    fontSize: TextUnit = TextUnit.Unspecified,
    lineHeight: TextUnit = TextUnit.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
    inlineContent: Map<String, InlineTextContent> = mapOf(),
    onTextLayout: (TextLayoutResult) -> Unit = {},
    ellipsisColor: Color = AppColors.blue,
    onClick: () -> Unit = {},
    animate: Boolean = true,
) {
    val style = remember(color, fontSize) {
        TextStyle.Default.copy(color = color, fontSize = fontSize)
    }
    val measurer = rememberTextMeasurer()

    var expand by remember { mutableStateOf(false) }
    // 源文本/行数/样式变化时重置为「未截断」，重新走一轮判定
    var cutAt by remember(text, maxLines, style) { mutableIntStateOf(-1) }

    val suffix = if (expand) ELLIPSIS_COLLAPSE else ELLIPSIS_MORE
    // 后缀只有两种取值，测一次即缓存
    val suffixWidth = remember(measurer, style, suffix) {
        measurer.measure(text = suffix, style = style).size.width
    }

    val displayText: AnnotatedString = remember(text, cutAt, expand, suffix, ellipsisColor) {
        buildAnnotatedString {
            when {
                expand -> {
                    append(text)
                    withStyle(SpanStyle(color = ellipsisColor)) { append(suffix) }
                }

                cutAt < 0 -> append(text)
                else -> {
                    append(text.subSequence(0, cutAt))
                    withStyle(SpanStyle(color = ellipsisColor)) { append(suffix) }
                }
            }
        }
    }

    Box(modifier = if (animate) Modifier.animateContentSize() else Modifier) {
        Text(
            text = displayText,
            modifier = Modifier
                .clickable(
                    onClick = {
                        onClick()
                        // 未截断时没有后缀可点，点击也不该切换展开态
                        if (expand || cutAt >= 0) expand = !expand
                    },
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ),
            lineHeight = lineHeight,
            overflow = TextOverflow.Ellipsis,
            maxLines = if (expand) Int.MAX_VALUE else maxLines,
            inlineContent = inlineContent,
            onTextLayout = { result ->
                onTextLayout(result)
                if (expand || maxLines == Int.MAX_VALUE) return@Text

                val lastLine = result.lineCount - 1
                // 未截断：保持现状（cutAt 若为 -1 就一直是 -1；若已截断说明上一轮截得正好）
                if (!result.isLineEllipsized(lastLine)) return@Text

                val lineEnd = result.getLineEnd(lastLine, visibleEnd = true)
                val lineStart = result.getLineStart(lastLine)
                val endX = result.getHorizontalPosition(lineEnd, true)
                // 从行尾往前退，直到腾出 ≥ 后缀宽度的空间
                var back = 0
                var freed = 0f
                while (lineEnd - back > lineStart && freed < suffixWidth + WIDTH_EPSILON) {
                    back++
                    freed = endX - result.getHorizontalPosition(lineEnd - back, true)
                }
                val next = (lineEnd - back).coerceAtLeast(lineStart)
                // 只允许变小：保证单调收敛，不会在两种截断位置间来回抖动
                if (cutAt !in 0..next) cutAt = next
            },
            style = style
        )
    }
}
