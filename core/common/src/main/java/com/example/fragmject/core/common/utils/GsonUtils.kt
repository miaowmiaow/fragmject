package com.example.fragmject.core.common.utils

import com.google.gson.ExclusionStrategy
import com.google.gson.FieldAttributes
import com.google.gson.Gson
import com.google.gson.GsonBuilder

/**
 * Gson 工具与单例集中管理。
 *
 * 设计动机：
 * 1. `Gson()` 看似轻量，构造时仍会反射构建 TypeAdapter 工厂表，频繁 `new` 会带来不必要的启动期/重组期开销；
 * 2. Gson 实例是线程安全的，全局单例最自然；
 * 3. 项目内同时存在两类 Gson 需求 —— 默认序列化、跳过 Kotlin `by lazy` 委托字段的序列化（缓存读写场景），
 *    在此统一暴露，避免散落在各处独立创建。
 */
object GsonUtils {

    /** 默认 Gson 单例：业务序列化/反序列化均可使用。 */
    val gson: Gson by lazy {
        GsonBuilder()
            .registerTypeAdapterFactory(NullSafeStringTypeAdapterFactory)
            .create()
    }

    /**
     * 实体序列化 Gson：跳过字段名含 `$` 的 Kotlin 合成字段（如 `by lazy` 生成的
     * `xxx$delegate`、Compose 的 `$stable` 等），避免 Gson 反序列化时尝试实例化
     * `kotlin.Lazy` 接口而崩溃。
     *
     * 用于 Room TypeConverter 等需要把实体 DTO 写盘 / 反序列化的场景。
     */
    val entityGson: Gson by lazy {
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