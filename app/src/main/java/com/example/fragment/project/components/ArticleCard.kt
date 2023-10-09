package com.example.fragment.project.components

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import coil.compose.AsyncImage
import com.example.fragment.project.R
import com.example.fragment.project.WanTheme
import com.example.fragment.project.bean.ArticleBean
import com.example.miaow.base.http.HttpRequest
import com.example.miaow.base.http.HttpResponse
import com.example.miaow.base.http.post
import kotlinx.coroutines.launch

@Composable
fun ArticleCard(
    data: ArticleBean,
    modifier: Modifier = Modifier,
    onWebCollect: (isAdd: Boolean, text: String) -> Unit = { _, _ -> },
    onNavigateToLogin: () -> Unit,
    onNavigateToSystem: (cid: String) -> Unit,
    onNavigateToUser: (userId: String) -> Unit,
    onNavigateToWeb: (url: String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var collectResId by remember(data.collect) { mutableIntStateOf(getCollectResId(data.collect)) }
    Box(modifier) {
        Card(elevation = 2.dp) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clickable(onClick = { onNavigateToWeb(data.link) })
            ) {
                Row(
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = data.avatarId),
                        contentDescription = null,
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .clickable { onNavigateToUser(data.userId) },
                        contentScale = ContentScale.Crop
                    )
                    ConstraintLayout(
                        modifier = Modifier
                            .height(35.dp)
                            .weight(1f)
                            .padding(start = 10.dp, end = 10.dp)
                    ) {
                        val (shareUser, niceDate) = createRefs()
                        Text(
                            text = "${data.author}${data.shareUser}".ifBlank { "匿名" },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = colorResource(R.color.text_666),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.constrainAs(shareUser) {
                                top.linkTo(parent.top)
                            })
                        Text(
                            text = data.niceDate,
                            fontSize = 12.sp,
                            color = colorResource(R.color.text_999),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.constrainAs(niceDate) {
                                bottom.linkTo(parent.bottom)
                            })
                    }
                    data.tags?.let { tags ->
                        if (tags.isNotEmpty()) {
                            Button(
                                onClick = {
                                    val uriString = "https://www.wanandroid.com${tags[0].url}"
                                    val uri = Uri.parse(uriString)
                                    var cid = uri.getQueryParameter("cid")
                                    if (cid.isNullOrBlank()) {
                                        val paths = uri.pathSegments
                                        if (paths != null && paths.size >= 3) {
                                            cid = paths[2]
                                        }
                                    }
                                    onNavigateToSystem(cid ?: "0")
                                },
                                modifier = Modifier.height(25.dp),
                                elevation = ButtonDefaults.elevation(0.dp, 0.dp, 0.dp),
                                shape = RoundedCornerShape(3.dp),
                                border = BorderStroke(1.dp, colorResource(R.color.blue)),
                                colors = ButtonDefaults.buttonColors(
                                    backgroundColor = colorResource(R.color.white),
                                    contentColor = colorResource(R.color.blue)
                                ),
                                contentPadding = PaddingValues(5.dp, 3.dp, 5.dp, 3.dp)
                            ) {
                                Text(
                                    text = tags[0].name,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.size(10.dp))
                Row(modifier = Modifier.padding(start = 16.dp, end = 16.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        if (data.desc.isNotBlank()) {
                            Text(
                                text = data.titleHtml,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = colorResource(R.color.text_333),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = data.descHtml,
                                fontSize = 14.sp,
                                color = colorResource(R.color.text_666),
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                            )
                        } else {
                            Text(
                                text = data.titleHtml,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = colorResource(R.color.text_333),
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    if (data.envelopePic.isNotBlank()) {
                        AsyncImage(
                            model = data.httpsEnvelopePic,
                            contentDescription = null,
                            modifier = Modifier
                                .padding(start = 10.dp)
                                .width(45.dp)
                                .aspectRatio(2f / 3f),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
                Spacer(Modifier.size(5.dp))
                Row(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 15.dp)
                    ) {
                        if (data.fresh) {
                            Text(
                                text = "新  ",
                                fontSize = 12.sp,
                                color = colorResource(R.color.blue),
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .height(20.dp)
                                    .clickable { onNavigateToSystem(data.chapterId) },
                            )
                        }
                        if (data.top) {
                            Text(
                                text = "置顶  ",
                                fontSize = 12.sp,
                                color = colorResource(R.color.orange),
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .height(20.dp)
                                    .clickable { onNavigateToSystem(data.chapterId) },
                            )
                        }
                        Text(
                            text = data.chapterNameHtml,
                            fontSize = 12.sp,
                            color = colorResource(R.color.text_999),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .height(20.dp)
                                .clickable { onNavigateToSystem(data.chapterId) },
                        )
                    }
                    Image(
                        painter = painterResource(id = collectResId),
                        contentDescription = "",
                        modifier = Modifier
                            .size(20.dp)
                            .clickable {
                                scope.launch {
                                    val request = HttpRequest().putPath("id", data.id)
                                    request.setUrl(
                                        if (data.collect)
                                            "lg/uncollect_originId/{id}/json"
                                        else
                                            "lg/uncollect_originId/{id}/json"
                                    )
                                    val response = post<HttpResponse>(request)
                                    when (response.errorCode) {
                                        "0" -> {
                                            data.collect = !data.collect
                                            collectResId = getCollectResId(data.collect)
                                            onWebCollect(data.collect, data.link)
                                        }

                                        "-1001" -> onNavigateToLogin()
                                    }
                                }
                            })
                }
            }
        }
    }
}

private fun getCollectResId(collect: Boolean): Int {
    return when (collect) {
        true -> R.mipmap.ic_collect_checked
        false -> R.mipmap.ic_collect_unchecked
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF0EAE2)
@Composable
fun ArticleCardPreview() {
    WanTheme {
        ArticleCard(
            data = ArticleBean(
                niceDate = "2022-10-13 11:11",
                title = "我是测试用户我是测试用户我是测试用户我是测试用户我是测试用户我是测试用户我是测试用户我是测试用户",
                desc = "我是测试内容我是测试内容我是测试我是测试内容我是测试内容我容我是测试内容我是测试内容我是测试内容我",
                fresh = true,
                top = true,
                superChapterName = "我是测试内容我是测试内容我是测试我是测试内容我是测试内容我容我是测试内容我是测试内容我是测试内容我",
                chapterName = "官方"
            ),
            onNavigateToLogin = {},
            onNavigateToSystem = {},
            onNavigateToUser = {},
            onNavigateToWeb = {},
        )
    }
}