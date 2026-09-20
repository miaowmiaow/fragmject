package com.example.fragmject.core.database.model

import com.example.fragmject.core.common.utils.GsonUtils
import com.google.gson.Gson

/**
 * 跨 Mapping 共享的 Gson 工具。
 *
 * 复用 [GsonUtils.entityGson]：排除 Kotlin 合成的 `$delegate`、`$stable` 等字段，
 * 避免 Gson 尝试反序列化 `kotlin.Lazy` 接口或 Compose 合成字段导致崩溃。
 */
object JsonConverters {
    val gson: Gson = GsonUtils.entityGson
}
