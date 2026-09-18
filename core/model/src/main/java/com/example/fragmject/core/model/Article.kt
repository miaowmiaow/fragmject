package com.example.fragmject.core.model

data class ShareArticle(
    val coinInfo: Coin? = null,
    val shareArticles: ArticleData? = null,
)

data class ArticleData(
    val curPage: String = "",
    val datas: List<Article>? = null,
    val offset: String = "",
    val over: Boolean = false,
    val pageCount: String = "0",
    val size: String = "",
    val total: String = ""
)

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
)

data class ArticleTag(
    val name: String = "",
    val url: String = ""
)