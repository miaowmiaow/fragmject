package com.example.fragmject.core.database.model

import com.example.fragmject.core.model.ProjectTree

fun ProjectTree.toEntity(cacheKey: String, sortOrder: Int) = ProjectTreeEntity(
    projectId = id,
    cacheKey = cacheKey,
    sortOrder = sortOrder,
    name = name,
    courseId = courseId,
    order = order,
    parentChapterId = parentChapterId,
    userControlSetTop = userControlSetTop,
    visible = visible,
    timestamp = System.currentTimeMillis(),
)

fun ProjectTreeEntity.toDomain() = ProjectTree(
    id = projectId,
    name = name,
    courseId = courseId,
    order = order,
    parentChapterId = parentChapterId,
    userControlSetTop = userControlSetTop,
    visible = visible,
)
