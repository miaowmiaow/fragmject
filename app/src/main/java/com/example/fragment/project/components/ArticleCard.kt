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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import com.example.fragment.library.base.R
import com.example.fragment.library.base.http.HttpRequest
import com.example.fragment.library.base.http.HttpResponse
import com.example.fragment.library.base.http.post
import com.example.fragment.project.WanTheme
import com.example.fragment.project.bean.ArticleBean
import kotlinx.coroutines.launch

@Composable
fun ArticleCard(
    data: ArticleBean,
    onNavigateToLogin: () -> Unit,
    onNavigateToSystem: (cid: String) -> Unit,
    onNavigateToUser: (userId: String) -> Unit,
    onNavigateToWeb: (url: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    var collectResId by rememberSaveable {
        mutableStateOf(
            if (data.collect) {
                R.drawable.ic_collect_checked
            } else {
                R.drawable.ic_collect_unchecked
            }
        )
    }

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
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .clickable { onNavigateToUser(data.userId) }
                    )
                    ConstraintLayout(
                        modifier = Modifier
                                .height(35.dp)
                                .weight(1f)
                                .padding(start = 10.dp, end = 10.dp)
                    ) {
                        val (share_user, nice_date) = createRefs()
                        Text(
                            text = "${data.author}${data.shareUser}".ifBlank { "匿名" },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = colorResource(R.color.text_666),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.constrainAs(share_user) {
                                top.linkTo(parent.top)
                            })
                        Text(
                            text = data.niceDate,
                            fontSize = 12.sp,
                            color = colorResource(R.color.text_999),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.constrainAs(nice_date) {
                                bottom.linkTo(parent.bottom)
                            })
                    }
                    if (!data.tags.isNullOrEmpty()) {
                        Button(
                            onClick = {
                                val uriString = "https://www.wanandroid.com${data.tags[0].url}"
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
                                text = data.tags[0].name,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
                Spacer(Modifier.size(10.dp))
                Row(modifier = Modifier.padding(start = 16.dp, end = 16.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        if (data.descHtml.isNotBlank()) {
                            Text(
                                text = data.titleHtml,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = colorResource(R.color.text_333),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = data.descHtml,
                                fontSize = 13.sp,
                                color = colorResource(R.color.text_666),
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                            )
                        } else {
                            Text(
                                text = data.titleHtml,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = colorResource(R.color.text_333),
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    if (data.httpsEnvelopePic.isNotBlank()) {
                        AsyncImage(
                            model = data.httpsEnvelopePic,
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
                                                if (data.collect) {
                                                    data.collect = false
                                                    collectResId = R.drawable.ic_collect_unchecked
                                                } else {
                                                    data.collect = true
                                                    collectResId = R.drawable.ic_collect_checked
                                                }
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