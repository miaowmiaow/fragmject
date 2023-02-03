package com.example.fragment.library.common.compose

import android.os.Build
import android.text.Html
import android.text.TextUtils
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import coil.compose.AsyncImage
import com.example.fragment.library.base.compose.theme.WanTheme
import com.example.fragment.library.base.http.HttpRequest
import com.example.fragment.library.base.http.HttpResponse
import com.example.fragment.library.base.http.post
import com.example.fragment.library.common.R
import com.example.fragment.library.common.bean.ArticleBean
import kotlinx.coroutines.launch
import java.util.regex.Pattern

@Composable
fun ArticleCard(
    modifier: Modifier = Modifier,
    item: ArticleBean,
    onClick: () -> Unit = {},
    avatarClick: () -> Unit = {},
    tagClick: () -> Unit = {},
    chapterNameClick: () -> Unit = {},
    onSignIn: () -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    var collectResId by rememberSaveable {
        mutableStateOf(
            if (item.collect) {
                R.drawable.ic_collect_checked
            } else {
                R.drawable.ic_collect_unchecked_stroke
            }
        )
    }

    val avatarList: List<Int> = listOf(
        R.drawable.avatar_1_raster,
        R.drawable.avatar_2_raster,
        R.drawable.avatar_3_raster,
        R.drawable.avatar_4_raster,
        R.drawable.avatar_5_raster,
        R.drawable.avatar_6_raster,
    )
    val shareUser = "${item.author}${item.shareUser}".ifBlank { "匿名" }
    val title = fromHtml(item.title)
    Box(modifier) {
        Card(elevation = 2.dp) {
            Column(
                Modifier
                    .clickable(onClick = onClick)
                    .padding(16.dp)
                    .fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = avatarList[item.id.toInt() % 6]),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .clickable(onClick = avatarClick)
                    )
                    ConstraintLayout(
                        modifier = Modifier
                            .weight(1f)
                            .height(35.dp)
                            .padding(start = 10.dp, end = 10.dp)
                    ) {
                        val (share_user, nice_date) = createRefs()
                        Text(
                            text = shareUser,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = colorResource(R.color.text_666),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.constrainAs(share_user) {
                                top.linkTo(parent.top)
                            })
                        Text(
                            text = item.niceDate,
                            fontSize = 12.sp,
                            color = colorResource(R.color.text_999),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.constrainAs(nice_date) {
                                bottom.linkTo(parent.bottom)
                            })
                    }
                    if (!item.tags.isNullOrEmpty()) {
                        Text(
                            text = item.tags[0].name,
                            fontSize = 12.sp,
                            color = colorResource(R.color.blue),
                            modifier = Modifier
                                .border(
                                    1.dp, colorResource(R.color.blue), RoundedCornerShape(3.dp)
                                )
                                .padding(5.dp, 3.dp, 5.dp, 3.dp)
                                .clickable(onClick = tagClick)
                        )
                    }
                }
                Spacer(Modifier.size(10.dp))
                Row {
                    Column(modifier = Modifier.weight(1f)) {
                        if (item.desc.isNotBlank()) {
                            Text(
                                text = title,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = colorResource(R.color.text_333),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = removeAllBank(fromHtml(item.desc), 2),
                                fontSize = 13.sp,
                                color = colorResource(R.color.text_666),
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                            )
                        } else {
                            Text(
                                text = title,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = colorResource(R.color.text_333),
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    if (item.envelopePic.isNotBlank()) {
                        AsyncImage(
                            model = item.getHttpsEnvelopePic(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .padding(start = 10.dp)
                                .width(45.dp)
                                .aspectRatio(2f / 3f)
                                .clip(RoundedCornerShape(16f))
                        )
                    }
                }
                Spacer(Modifier.size(5.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = buildAnnotatedString {
                            if (item.fresh) {
                                withStyle(style = SpanStyle(color = colorResource(R.color.blue))) {
                                    append("新  ")
                                }
                            }
                            if (item.top) {
                                withStyle(
                                    style = SpanStyle(color = colorResource(R.color.orange))
                                ) {
                                    append("置顶  ")
                                }
                            }
                            append(
                                fromHtml(
                                    formatChapterName(
                                        item.superChapterName, item.chapterName
                                    )
                                )
                            )
                        },
                        fontSize = 12.sp,
                        color = colorResource(R.color.text_999),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .padding(end = 35.dp)
                            .clickable(onClick = chapterNameClick),
                    )
                    Spacer(
                        Modifier
                            .height(20.dp)
                            .weight(1f)
                    )
                    Image(
                        painter = painterResource(id = collectResId),
                        contentDescription = "",
                        modifier = Modifier
                            .size(20.dp)
                            .clickable {
                                scope.launch {
                                    if (item.collect) {
                                        val url = "lg/uncollect_originId/{id}/json"
                                        val request = HttpRequest(url).putPath("id", item.id)
                                        val response = post<HttpResponse>(request)
                                        if (response.errorCode == "0") {
                                            item.collect = false
                                            collectResId = R.drawable.ic_collect_unchecked_stroke
                                        } else if (response.errorCode == "-1001") {
                                            onSignIn()
                                        }
                                    } else {
                                        val request = HttpRequest("lg/collect/{id}/json")
                                            .putPath("id", item.id)
                                        val response = post<HttpResponse>(request)
                                        if (response.errorCode == "0") {
                                            item.collect = true
                                            collectResId = R.drawable.ic_collect_checked
                                        } else if (response.errorCode == "-1001") {
                                            onSignIn()
                                        }
                                    }
                                }
                            })
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF0EAE2)
@Composable
fun ArticleCardPreview() {
    WanTheme {
        ArticleCard(
            item = ArticleBean(
                niceDate = "2022-10-13 11:11",
                title = "我是测试用户我是测试用户我是测试用户我是测试用户我是测试用户我是测试用户我是测试用户我是测试用户",
                desc = "我是测试内容我是测试内容我是测试我是测试内容我是测试内容我容我是测试内容我是测试内容我是测试内容我",
                fresh = true,
                top = true,
                superChapterName = "问答",
                chapterName = "官方"
            ),
            onClick = {}
        )
    }
}

private fun fromHtml(str: String): String {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        Html.fromHtml(str, Html.FROM_HTML_MODE_LEGACY).toString()
    } else {
        Html.fromHtml(str).toString()
    }
}

private fun formatChapterName(vararg names: String): String {
    val format = StringBuilder()
    for (name in names) {
        if (!TextUtils.isEmpty(name)) {
            if (format.isNotEmpty()) format.append(" · ")
            format.append(name)
        }
    }
    return format.toString()
}

private fun removeAllBank(str: String?, count: Int): String {
    var s = ""
    if (str != null) {
        val p = Pattern.compile("\\s{$count,}|\t|\r|\n")
        val m = p.matcher(str)
        s = m.replaceAll(" ")
    }
    return s
}