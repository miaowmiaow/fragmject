package com.example.fragmject.core.database.model

import com.google.gson.ExclusionStrategy
import com.google.gson.FieldAttributes
import com.google.gson.Gson
import com.google.gson.GsonBuilder

/**
 * 跨 Mapping 共享的 Gson 工具。
 *
 * 配置要点：排除 Kotlin 合成的 `$delegate` 等字段，
 * 避免 Gson 尝试反序列化 `kotlin.Lazy` 接口导致崩溃。
 * 详见 Article.titleHtml / descHtml 等 `by lazy` 属性。
 */
object ArticleTypeConverters {
    val gson: Gson = GsonBuilder()
        .addSerializationExclusionStrategy(KotlinSyntheticExclusion)
        .addDeserializationExclusionStrategy(KotlinSyntheticExclusion)
        .create()
}

/** 排除含 `$` 字符的字段名（Kotlin 编译器为 delegate / 伴生对象生成的合成字段）。 */
private object KotlinSyntheticExclusion : ExclusionStrategy {
    override fun shouldSkipClass(clazz: Class<*>?) = false

    override fun shouldSkipField(f: FieldAttributes?) =
        f?.name?.contains('$') == true
}
