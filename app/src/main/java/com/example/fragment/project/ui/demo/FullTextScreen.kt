package com.example.fragment.project.ui.demo

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.fragment.project.R
import com.example.fragment.project.WanTheme
import com.example.fragment.project.components.EllipsisText

@Composable
fun FullTextScreen() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column {
            var ellipsis1 by remember { mutableStateOf(false) }
            var expand1 by remember { mutableStateOf(false) }
            var ellipsis2 by remember { mutableStateOf(false) }
            var expand2 by remember { mutableStateOf(false) }
            Box(modifier = Modifier.padding(10.dp)) {
                EllipsisText(
                    text = buildAnnotatedString {
                        appendInlineContent("icon")
                        append(
                            "壬戌之秋1，七月既望2，苏子与客泛舟游于赤壁之下。清风徐来3，水波不兴4。举酒属客5，诵明月之诗6，歌窈窕之章7。少焉8，月出于东山之上，徘徊于斗牛之间9。"
                        )
                    },
                    color = colorResource(R.color.text_333),
                    backgroundColor = colorResource(R.color.background),
                    fontSize = 14.sp,
                    maxLines = if (expand1) Int.MAX_VALUE else 2,
                    inlineContent = mapOf(
                        Pair(
                            "icon",
                            InlineTextContent(
                                Placeholder(
                                    width = 1.em,
                                    height = 1.em,
                                    placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter
                                )
                            ) {
                                AsyncImage(
                                    model = R.mipmap.avatar_1_raster,
                                    contentDescription = null,
                                    contentScale = ContentScale.Fit,
                                )
                            }
                        )
                    ),
                    onTextLayout = {
                        ellipsis1 = it.isLineEllipsized(it.lineCount - 1)
                    },
                    ellipsisText = if (expand1) {
                        "...收起"
                    } else if (ellipsis1) {
                        "...展开"
                    } else {
                        ""
                    }
                ) {
                    expand1 = !expand1
                }
            }
            Box(modifier = Modifier.padding(10.dp)) {
                EllipsisText(
                    text = buildAnnotatedString {
                        append(
                            "I am happy to join with you today in what will go down in history as the greatest demonstration for freedom in the history of our nation."
                        )
                    },
                    color = colorResource(R.color.text_333),
                    backgroundColor = colorResource(R.color.background),
                    fontSize = 14.sp,
                    maxLines = if (expand2) Int.MAX_VALUE else 2,
                    onTextLayout = {
                        ellipsis2 = it.isLineEllipsized(it.lineCount - 1)
                    },
                    ellipsisText = if (expand2) {
                        "...收起"
                    } else if (ellipsis2) {
                        "...展开"
                    } else {
                        ""
                    }
                ) {
                    expand2 = !expand2
                }
            }
        }
    }

}

@Preview(showBackground = true, backgroundColor = 0xFFF0F0F0)
@Composable
fun FullTextScreenPreview() {
    WanTheme { FullTextScreen() }
}