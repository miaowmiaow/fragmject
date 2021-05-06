package com.example.fragment.module.home.bean

import com.example.fragment.library.base.http.HttpResponse

data class ArticleBean(
    val data: List<ArticleDataBean>? = null
) : HttpResponse()

data class ArticleDataBean(
    val curPage:  Int? = null,
    val datas: List<ArticleDatasBean>,
    val offset:  Int? = null,
    val over:  Boolean? = null,
    val pageCount:  Int? = null,
    val size:  Int? = null,
    val total:  Int? = null
)

data class ArticleDatasBean(
    val apkLink:  String? = null,
    val audit:  Int? = null,
    val author:  String? = null,
    val canEdit:  Boolean? = null,
    val chapterId:  Int? = null,
    val chapterName:  String? = null,
    val collect:  Boolean? = null,
    val courseId:  Int? = null,
    val desc:  String? = null,
    val descMd:  String? = null,
    val envelopePic:  String? = null,
    val fresh:  Boolean? = null,
    val host:  String? = null,
    val id:  Int? = null,
    val link:  String? = null,
    val niceDate:  String? = null,
    val niceShareDate:  String? = null,
    val origin:  String? = null,
    val prefix:  String? = null,
    val projectLink:  String? = null,
    val publishTime: Long,
    val realSuperChapterId:  Int? = null,
    val selfVisible:  Int? = null,
    val shareDate: Long,
    val shareUser:  String? = null,
    val superChapterId:  Int? = null,
    val superChapterName:  String? = null,
    val tags: List<ArticleTagBean>,
    val title:  String? = null,
    val type:  Int? = null,
    val userId:  Int? = null,
    val visible:  Int? = null,
    val zan:  Int? = null
)

data class ArticleTagBean(
    val name:  String? = null,
    val url:  String? = null
)