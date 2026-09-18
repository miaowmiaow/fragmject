package com.example.fragmject.core.model

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
)
