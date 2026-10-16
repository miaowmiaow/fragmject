package com.example.fragmject.core.model

data class Tree(
    val children: List<Tree>? = null,
    // 原 var childrenSelectPosition 已移除：全项目零使用，且它是 Tree 无法满足
    // compose_stability_config.conf 收录条件（字段须全为 val）的唯一原因。移除后 Tree 已收录。
    val courseId: String = "",
    val id: String = "",
    val name: String = "",
    val order: String = "",
    val parentChapterId: String = "",
    val userControlSetTop: String = "",
    val visible: String = ""
)
