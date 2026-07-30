package com.example.fragmject.core.database.model

import com.example.fragmject.core.model.HotKey
import com.google.gson.Gson

fun HotKey.toEntity(cacheKey: String, sortOrder: Int): HotKeyEntity = HotKeyEntity(
    hotKeyId = id,
    cacheKey = cacheKey,
    sortOrder = sortOrder,
    name = name,
    link = link,
    order = order,
    visible = visible,
    timestamp = System.currentTimeMillis(),
)

fun HotKeyEntity.toDomain(): HotKey = HotKey(
    id = hotKeyId,
    name = name,
    link = link,
    order = order,
    visible = visible,
)
