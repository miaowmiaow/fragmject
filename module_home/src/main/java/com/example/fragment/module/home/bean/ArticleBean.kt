package com.example.fragment.module.home.bean

import com.example.fragment.library.base.http.HttpResponse

data class ArticleBean(
    val data: ArticleListBean? = null
) : HttpResponse()

data class TopArticleBean(
    val data: List<ArticleDataBean>? = null
) : HttpResponse()

data class ArticleListBean(
    val curPage: String = "",
    val datas: List<ArticleDataBean>? = null,
    val offset: String = "",
    val over: Boolean = false,
    val pageCount: String = "",
    val size: String = "",
    val total: String = ""
)

data class ArticleDataBean(
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
    var top: Boolean = false,
    val fresh: Boolean = false,
    val host: String = "",
    val id: String = "",
    val link: String = "",
    val niceDate: String = "",
    val niceShareDate: String = "",
    val origin: String = "",
    val prefix: String = "",
    val projectLink: String = "",
    val publishTime: Long,
    val realSuperChapterId: String = "",
    val selfVisible: String = "",
    val shareDate: Long,
    val shareUser: String = "",
    val superChapterId: String = "",
    val superChapterName: String = "",
    val tags: List<ArticleTagBean>? = null,
    val title: String = "",
    val type: String = "",
    val userId: String = "",
    val visible: String = "",
    val zan: String = ""
)

data class ArticleTagBean(
    val name: String = "",
    val url: String = ""
)