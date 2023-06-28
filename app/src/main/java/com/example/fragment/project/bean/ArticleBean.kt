package com.example.fragment.project.bean

import android.os.Build
import android.os.Parcelable
import android.text.Html
import android.text.TextUtils
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.example.fragment.library.base.R
import com.example.fragment.library.base.http.HttpResponse
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import java.util.regex.Pattern
import kotlin.math.abs

@Parcelize
data class ShareArticleListBean(
    val data: ShareArticleBean? = null
) : HttpResponse(), Parcelable

@Parcelize
data class ShareArticleBean(
    val coinInfo: CoinBean? = null,
    val shareArticles: ArticleDataBean? = null,
) : Parcelable

@Parcelize
data class ArticleListBean(
    val data: ArticleDataBean? = null
) : HttpResponse(), Parcelable

@Parcelize
data class TopArticleBean(
    val data: List<ArticleBean>? = null
) : HttpResponse(), Parcelable

@Parcelize
data class ArticleDataBean(
    val curPage: String = "",
    val datas: List<ArticleBean>? = null,
    val offset: String = "",
    val over: Boolean = false,
    val pageCount: String = "",
    val size: String = "",
    val total: String = ""
) : Parcelable

@Parcelize
data class ArticleBean(
    val apkLink: String = "",
    val audit: String = "",
    val author: String = "",
    val canEdit: Boolean = false,
    val chapterId: String = "",
    private val chapterName: String = "",
    var collect: Boolean = false,
    val courseId: String = "",
    private val desc: String = "",
    val descMd: String = "",
    private val envelopePic: String = "",
    var top: Boolean = false,
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
    private val superChapterName: String = "",
    val tags: List<ArticleTagBean>? = null,
    private val title: String = "",
    val type: String = "",
    val userId: String = "",
    val visible: String = "",
    val zan: String = "",
    val banners: List<BannerBean>? = null,
    var viewType: Int = 1
) : Parcelable {

    @IgnoredOnParcel
    private val avatarList: List<Int> = listOf(
        R.drawable.avatar_1_raster,
        R.drawable.avatar_2_raster,
        R.drawable.avatar_3_raster,
        R.drawable.avatar_4_raster,
        R.drawable.avatar_5_raster,
        R.drawable.avatar_6_raster,
    )

    @IgnoredOnParcel
    var avatarId: Int = R.drawable.avatar_1_raster

    @IgnoredOnParcel
    var titleHtml: String = ""

    @IgnoredOnParcel
    var descHtml: String = ""

    @IgnoredOnParcel
    var chapterNameHtml: AnnotatedString = buildAnnotatedString {}

    @IgnoredOnParcel
    var httpsEnvelopePic: String = ""

    fun build(): ArticleBean {
        avatarId = avatarList[
                try {
                    abs(userId.toInt()) % 6
                } catch (e: Exception) {
                    e.printStackTrace()
                    0
                }
        ]
        titleHtml = fromHtml(title)
        descHtml = removeAllBank(fromHtml(desc), 2)
        chapterNameHtml = buildAnnotatedString {
            if (fresh) {
                withStyle(style = SpanStyle(color = Color(0xFF508CEE))) {
                    append("新  ")
                }
            }
            if (top) {
                withStyle(
                    style = SpanStyle(color = Color(0xFFFF7800))
                ) {
                    append("置顶  ")
                }
            }
            append(
                fromHtml(formatChapterName(superChapterName, chapterName))
            )
        }
        httpsEnvelopePic = envelopePic.replace("http://", "https://")
        return this
    }

    private fun fromHtml(str: String): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Html.fromHtml(str, Html.FROM_HTML_MODE_LEGACY).toString()
        } else {
            Html.fromHtml(str).toString()
        }
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
}

@Parcelize
data class ArticleTagBean(
    val name: String = "",
    val url: String = ""
) : Parcelable