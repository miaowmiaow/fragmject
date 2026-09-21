package com.example.fragmject.core.database.model

import com.google.gson.ExclusionStrategy
import com.google.gson.FieldAttributes
import com.google.gson.Gson
import com.google.gson.GsonBuilder

/**
 * 跨 Mapping 共享的 Gson 工具。
 *
 * 排除 Kotlin 合成的 `$delegate`、`$stable` 等字段，
 * 避免 Gson 尝试反序列化 `kotlin.Lazy` 接口或 Compose 合成字段导致崩溃。
 *
 * 数据库缓存场景由自身 toJson 序列化、字段永远齐全，无需 null-safe 策略。
 */
object JsonConverters {
    val gson: Gson by lazy {
        GsonBuilder()
            .addSerializationExclusionStrategy(SyntheticFieldExclusion)
            .addDeserializationExclusionStrategy(SyntheticFieldExclusion)
            .create()
    }
}

/**
 * 排除字段名含 `$` 的 Kotlin 合成字段（delegate / 伴生 / Compose stable 等）。
 */
private object SyntheticFieldExclusion : ExclusionStrategy {
    override fun shouldSkipField(f: FieldAttributes): Boolean =
        f.name?.contains('$') == true

    override fun shouldSkipClass(clazz: Class<*>): Boolean = false
}
