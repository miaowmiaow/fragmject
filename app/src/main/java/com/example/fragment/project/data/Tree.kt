package com.example.fragment.project.data

import android.os.Parcelable
import com.example.miaow.base.http.HttpResponse
import kotlinx.parcelize.Parcelize

@Parcelize
data class TreeList(
    val data: MutableList<Tree>? = null,
) : HttpResponse(), Parcelable

@Parcelize
data class Tree(
    val children: List<Tree>? = null,
    var childrenSelectPosition: Int = 0,
    val courseId: String = "",
    val id: String = "",
    val name: String = "",
    val order: String = "",
    val parentChapterId: String = "",
    val userControlSetTop: String = "",
    val visible: String = ""
) : Parcelable
