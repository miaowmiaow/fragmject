package com.example.fragment.project.bean

import android.os.Parcelable
import com.example.miaow.base.http.HttpResponse
import kotlinx.parcelize.Parcelize

@Parcelize
data class TreeListBean(
    val data: List<TreeBean>? = null,
) : HttpResponse(), Parcelable

@Parcelize
data class TreeBean(
    val children: List<TreeBean>? = null,
    var childrenSelectPosition: Int = 0,
    val courseId: String = "",
    val id: String = "",
    val name: String = "",
    val order: String = "",
    val parentChapterId: String = "",
    val userControlSetTop: String = "",
    val visible: String = ""
) : Parcelable
