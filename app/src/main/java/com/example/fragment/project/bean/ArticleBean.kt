package com.example.fragment.project.bean

import android.os.Parcelable
import com.example.fragment.library.base.R
import com.example.fragment.library.base.http.HttpResponse
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

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
    val avatarList: List<Int> = listOf(
        R.drawable.avatar_1_raster,
        R.drawable.avatar_2_raster,
        R.drawable.avatar_3_raster,
        R.drawable.avatar_4_raster,
        R.drawable.avatar_5_raster,
        R.drawable.avatar_6_raster,
    )

    fun getAvatarId(): Int {
        var index = 0
        try {
            val id = userId.toInt()
            if (id >= 0) {
                index = id % 6
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return avatarList[index]
    }

    fun getHttpsEnvelopePic(): String {
        return envelopePic.replace("http://", "https://")
    }

}

@Parcelize
data class ArticleTagBean(
    val name: String = "",
    val url: String = ""
) : Parcelable