package com.example.fragment.project.bean

import com.example.fragment.library.base.http.HttpResponse

data class ProjectTreeListBean(
    val data: List<ProjectTreeBean>? = null,
) : HttpResponse()

data class ProjectTreeBean(
    val courseId: String = "",
    val id: String = "",
    val name: String = "",
    val order: String = "",
    val parentChapterId: String = "",
    val userControlSetTop: String = "",
    val visible: String = ""
)