package com.example.fragment.project.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import coil.compose.AsyncImage
import com.example.fragment.project.R
import com.example.fragment.project.WanTheme
import com.example.fragment.project.data.Article
import com.example.miaow.base.http.HttpResponse
import com.example.miaow.base.http.post
import kotlinx.coroutines.launch

@Composable
fun ArticleCard(
    data: Article,
    modifier: Modifier = Modifier,
    onNavigateToLogin: () -> Unit,
    onNavigateToSystem: (cid: String) -> Unit,
    onNavigateToUser: (userId: String) -> Unit,
    onNavigateToWeb: (url: String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var collectResId by remember(data.collect) { mutableIntStateOf(getCollectResId(data.collect)) }
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(5.dp))
            .clipToBounds()
            .clickable { onNavigateToWeb(data.link) }
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = data.avatarId),
                contentDescription = null,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { onNavigateToUser(data.userId) }
                    .size(30.dp),
                contentScale = ContentScale.Crop
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .wrapContentHeight()
                    .padding(start = 10.dp, end = 10.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${data.author}${data.shareUser}".ifBlank { "匿名" },
                    color = MaterialTheme.colorScheme.onSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = data.niceDate,
                    color = MaterialTheme.colorScheme.onSecondary,
                    fontSize = 12.sp,
                    lineHeight = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            data.tags?.let { tags ->
                if (tags.isNotEmpty()) {
                    OutlinedButton(
                        onClick = {
                            val uriString = "https://www.wanandroid.com${tags[0].url}"
                            val uri = uriString.toUri()
                            var cid = uri.getQueryParameter("cid")
                            if (cid.isNullOrBlank()) {
                                val paths = uri.pathSegments
                                if (paths != null && paths.size >= 3) {
                                    cid = paths[2]
                                }
                            }
                            onNavigateToSystem(cid ?: "0")
                        },
                        modifier = Modifier.height(20.dp),
                        shape = RoundedCornerShape(3.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                        ),
                        elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onTertiaryContainer),
                        contentPadding = PaddingValues(3.dp, 2.dp, 3.dp, 2.dp)
                    ) {
                        Text(
                            text = tags[0].name,
                            fontSize = 12.sp,
                            lineHeight = 12.sp
                        )
                    }
                }
            }
        }
        Spacer(Modifier.size(10.dp))
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                if (data.desc.isNotBlank()) {
                    Text(
                        text = data.titleHtml,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = data.descHtml,
                        color = MaterialTheme.colorScheme.onSecondary,
                        fontSize = 14.sp,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                } else {
                    Text(
                        text = data.titleHtml,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (data.envelopePic.isNotBlank()) {
                AsyncImage(
                    model = data.httpsEnvelopePic,
                    contentDescription = null,
                    modifier = Modifier
                        .width(60.dp)
                        .padding(start = 10.dp)
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
            val footModifier = Modifier.wrapContentHeight()
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 15.dp)
            ) {
                if (data.fresh) {
                    Text(
                        text = "新  ",
                        modifier = footModifier.clickable { onNavigateToSystem(data.chapterId) },
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        fontSize = 12.sp,
                        lineHeight = 12.sp,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (data.top) {
                    Text(
                        text = "置顶  ",
                        modifier = footModifier.clickable { onNavigateToSystem(data.chapterId) },
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        fontSize = 12.sp,
                        lineHeight = 12.sp,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = data.chapterNameHtml,
                    modifier = footModifier.clickable { onNavigateToSystem(data.chapterId) },
                    color = MaterialTheme.colorScheme.onTertiary,
                    fontSize = 12.sp,
                    lineHeight = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Image(
                painter = painterResource(id = collectResId),
                contentDescription = "",
                modifier = footModifier
                    .height(20.dp)
                    .clickable {
                        scope.launch {
                            val response = post<HttpResponse> {
                                setUrl(
                                    if (data.collect) {
                                        "lg/collect/{id}/json"
                                    } else {
                                        "lg/uncollect_originId/{id}/json"
                                    }
                                )
                                putPath("id", data.id)
                            }
                            when (response.errorCode) {
                                "0" -> {
                                    data.collect = !data.collect
                                    collectResId = getCollectResId(data.collect)
                                }

                                "-1001" -> onNavigateToLogin()
                            }
                        }
                    })
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
            data = Article(
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