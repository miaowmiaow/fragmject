package com.example.fragmject.core.model

import android.os.Parcelable
import android.text.Html
import com.example.fragmject.core.network.http.HttpResponse
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import androidx.core.net.toUri

@Parcelize
data class ShareArticleList(
    val data: ShareArticle? = null
) : HttpResponse(), Parcelable

@Parcelize
data class ShareArticle(
    val coinInfo: Coin? = null,
    val shareArticles: ArticleData? = null,
) : Parcelable

@Parcelize
data class ArticleList(
    val data: ArticleData? = null
) : HttpResponse(), Parcelable

@Parcelize
data class TopArticle(
    val data: List<Article>? = null
) : HttpResponse(), Parcelable

@Parcelize
data class ArticleData(
    val curPage: String = "",
    val datas: List<Article>? = null,
    val offset: String = "",
    val over: Boolean = false,
    val pageCount: String = "0",
    val size: String = "",
    val total: String = ""
) : Parcelable

@Parcelize
data class Article(
    val apkLink: String = "",
    val audit: String = "",
    val author: String = "",
    val canEdit: Boolean = false,
    val chapterId: String = "",
    val chapterName: String = "",
    val collect: Boolean = false,
    val courseId: String = "",
    val desc: String = "",
    val descMd: String = "",
    val envelopePic: String = "",
    val top: Boolean = false,
    val fresh: Boolean = false,
    val host: String = "",
    val id: String = "0",
    val link: String = "",
    val niceDate: String = "",
    val niceShareDate: String = "",
    val origin: String = "",
    val prefix: String = "",
    val projectLink: String = "",
    val publishTime: String = "",
    val realSuperChapterId: String = "",
    val selfVisible: String = "",
    val shareDate: String = "",
    val shareUser: String = "",
    val superChapterId: String = "",
    val superChapterName: String = "",
    val tags: List<ArticleTag>? = null,
    val title: String = "",
    val type: String = "",
    val userId: String = "0",
    val visible: String = "",
    val zan: String = "",
    val banners: List<Banner>? = null,
    val viewType: Int = 1
) : Parcelable {

    @IgnoredOnParcel
    val titleHtml by lazy {
        fromHtml(title)
    }

    @IgnoredOnParcel
    val descHtml by lazy {
        fromHtml(desc)
    }

    @IgnoredOnParcel
    val chapterNameHtml by lazy {
        fromHtml(formatChapterName(superChapterName, chapterName))
    }

    @IgnoredOnParcel
    val httpsEnvelopePic by lazy {
        envelopePic.replace("http://", "https://")
    }

    /**
     * 预加载所有 UI 展示所需的计算（Html 解析等），确保后续调用
     * [toArticleCardUiState] 时不再触发 JNI 调用，避免 Compose 重组掉帧。
     *
     * 应在数据到达 ViewModel 时、写入 composition 可见列表之前调用。
     */
    fun preloadForDisplay() {
        titleHtml; descHtml; chapterNameHtml; httpsEnvelopePic
    }

    private fun fromHtml(str: String): String {
        return Html.fromHtml(str, Html.FROM_HTML_MODE_LEGACY).toString()
    }

    private fun formatChapterName(vararg names: String): String {
        val stringBuilder = StringBuilder()
        for ((index, name) in names.withIndex()) {
            if (index > 0) stringBuilder.append("·")
            stringBuilder.append(name)
        }
        return stringBuilder.toString()
    }
}

@Parcelize
data class ArticleTag(
    val name: String = "",
    val url: String = ""
) : Parcelable

/**
 * 从 [ArticleTag.url] 中提取文章分类 cid。
 *
 * URL 格式通常为 `https://www.wanandroid.com/project/list/1?cid=294`，
 * 优先通过 query 参数 "cid" 获取，其次从 pathSegments 提取。
 */
fun ArticleTag.extractCid(): String {
    val uriString = "https://www.wanandroid.com$url"
    val uri = uriString.toUri()
    var cid = uri.getQueryParameter("cid")
    if (cid.isNullOrBlank()) {
        val paths = uri.pathSegments
        if (paths != null && paths.size >= 3) {
            cid = paths[2]
        }
    }
    return cid ?: "0"
}