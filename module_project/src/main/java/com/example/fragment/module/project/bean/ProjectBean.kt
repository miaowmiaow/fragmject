package com.example.fragment.module.project.bean

import com.example.fragment.library.base.http.HttpResponse

data class ProjectTreeBean(
    val data: List<ProjectTreeDataBean>? = null,
) : HttpResponse()

data class ProjectTreeDataBean(
    val courseId: String = "",
    val id: String = "",
    val name: String = "",
    val order: String = "",
    val parentChapterId: String = "",
    val userControlSetTop: String = "",
    val visible: String = ""
)

data class ProjectListBean(
    val data: ProjectListDataBean? = null,
) : HttpResponse()

data class ProjectListDataBean(
    val curPage: String = "",
    val datas: List<ProjectBean>? = null,
    val offset: String = "",
    val over: String = "",
    val pageCount: String = "",
    val size: String = "",
    val total: String = "",
)

data class ProjectBean(
    val apkLink: String = "",
    val audit: String = "",
    val author: String = "",
    val canEdit: String = "",
    val chapterId: String = "",
    val chapterName: String = "",
    val collect: String = "",
    val courseId: String = "",
    val desc: String = "",
    val descMd: String = "",
    val envelopePic: String = "",
    val fresh: String = "",
    val host: String = "",
    val id: String = "",
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
    val tags: List<ProjectTagBean>,
    val title: String = "",
    val type: String = "",
    val userId: String = "",
    val visible: String = "",
    val zan: String = ""
)

data class ProjectTagBean(
    val name: String = "",
    val url: String = ""
)