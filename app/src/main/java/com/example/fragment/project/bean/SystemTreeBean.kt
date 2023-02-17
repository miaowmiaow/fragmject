package com.example.fragment.project.bean

import android.os.Parcelable
import com.example.fragment.library.base.http.HttpResponse
import kotlinx.parcelize.Parcelize

@Parcelize
data class SystemTreeListBean(
    val data: List<SystemTreeBean>? = null,
) : HttpResponse(), Parcelable

@Parcelize
data class SystemTreeBean(
    val children: List<SystemTreeBean>? = null,
    var childrenSelectPosition: Int = 0,
    val courseId: String = "",
    val id: String = "",
    val name: String = "",
    val order: String = "",
    val parentChapterId: String = "",
    val userControlSetTop: String = "",
    val visible: String = ""
) : Parcelable
