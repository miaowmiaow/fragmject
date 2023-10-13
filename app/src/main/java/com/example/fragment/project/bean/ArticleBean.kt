package com.example.fragment.project.bean

import android.os.Parcelable
import android.text.Html
import android.util.Log
import com.example.fragment.project.R
import com.example.miaow.base.http.HttpResponse
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
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
    val chapterName: String = "",
    var collect: Boolean = false,
    val courseId: String = "",
    val desc: String = "",
    val descMd: String = "",
    val envelopePic: String = "",
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
    val superChapterName: String = "",
    val tags: List<ArticleTagBean>? = null,
    val title: String = "",
    val type: String = "",
    val userId: String = "",
    val visible: String = "",
    val zan: String = "",
    val banners: List<BannerBean>? = null,
    var viewType: Int = 1
) : Parcelable {

    @IgnoredOnParcel
    private val avatarList: List<Int> = listOf(
        R.mipmap.avatar_1_raster,
        R.mipmap.avatar_2_raster,
        R.mipmap.avatar_3_raster,
        R.mipmap.avatar_4_raster,
        R.mipmap.avatar_5_raster,
        R.mipmap.avatar_6_raster,
    )

    @IgnoredOnParcel
    val avatarId by lazy {
        try {
            avatarList[abs(userId.toInt()) % 6]
        } catch (e: Exception) {
            Log.e(this.javaClass.name, e.message.toString())
            1
        }
    }

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
data class ArticleTagBean(
    val name: String = "",
    val url: String = ""
) : Parcelable