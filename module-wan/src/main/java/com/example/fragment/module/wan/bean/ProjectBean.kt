package com.example.fragment.module.wan.bean

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