package com.example.fragment.project.bean

import android.os.Parcelable
import android.text.Html
import android.text.TextUtils
import com.example.fragment.library.base.R
import com.example.fragment.library.base.http.HttpResponse
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
        R.drawable.avatar_1_raster,
        R.drawable.avatar_2_raster,
        R.drawable.avatar_3_raster,
        R.drawable.avatar_4_raster,
        R.drawable.avatar_5_raster,
        R.drawable.avatar_6_raster,
    )

    @IgnoredOnParcel
    private var avatarId: Int? = null

    fun getAvatarId(): Int {
        if (avatarId == null) {
            avatarId = avatarList[abs(userId.toInt()) % 6]
        }
        return avatarId ?: -1
    }

    @IgnoredOnParcel
    private var titleHtml: String? = null

    fun getTitleHtml(): String {
        if (titleHtml == null) {
            titleHtml = fromHtml(title)
        }
        return titleHtml ?: ""
    }

    @IgnoredOnParcel
    private var descHtml: String? = null

    fun getDescHtml(): String {
        if (descHtml == null) {
            descHtml = fromHtml(desc)
        }
        return descHtml ?: ""
    }

    @IgnoredOnParcel
    private var chapterNameHtml: String? = null

    fun getChapterNameHtml(): String {
        if (chapterNameHtml == null) {
            chapterNameHtml = fromHtml(formatChapterName(superChapterName, chapterName))
        }
        return chapterNameHtml ?: ""
    }

    @IgnoredOnParcel
    private var httpsEnvelopePic: String? = null

    fun getHttpsEnvelopePic(): String {
        if (httpsEnvelopePic == null) {
            httpsEnvelopePic = envelopePic.replace("http://", "https://")
        }
        return httpsEnvelopePic ?: envelopePic
    }

    private fun fromHtml(str: String): String {
        return Html.fromHtml(str, Html.FROM_HTML_MODE_LEGACY).toString()
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