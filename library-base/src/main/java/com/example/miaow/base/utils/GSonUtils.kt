package com.example.miaow.base.utils

import com.google.gson.ExclusionStrategy
import com.google.gson.FieldAttributes
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

/**
 * Gson 工具与单例集中管理。
 *
 * 设计动机：
 * 1. `Gson()` 看似轻量，构造时仍会反射构建 TypeAdapter 工厂表，频繁 `new` 会带来不必要的启动期/重组期开销；
 * 2. Gson 实例是线程安全的，全局单例最自然；
 * 3. 项目内同时存在两类 Gson 需求 —— 默认序列化、跳过 Kotlin `by lazy` 委托字段的序列化（缓存读写场景），
 *    在此统一暴露，避免散落在各处独立创建。
 */
object GSonUtils {

    /** 默认 Gson 单例：业务序列化/反序列化均可使用。 */
    val gson: Gson by lazy { Gson() }

    /**
     * Lazy-aware Gson：跳过 Kotlin `by lazy` 委托属性合成的 `xxx$delegate` 字段
     * （类型为接口 `kotlin.Lazy`）。
     *
     * 用于接口响应缓存等需要把 DTO 写盘 / 反序列化的场景，避免：
     *  - 序列化时把派生属性的 lazy 持有对象写入缓存；
     *  - 反序列化时 Gson 试图 `new` `kotlin.Lazy` 接口而崩溃。
     */
    val lazyAwareGson: Gson by lazy {
        GsonBuilder()
            .addSerializationExclusionStrategy(LazyDelegateExclusion)
            .addDeserializationExclusionStrategy(LazyDelegateExclusion)
            .create()
    }

    fun <T> fromJson(json: String, raw: Class<*>, vararg args: Type): T {
        val type = object : ParameterizedType {
            override fun getRawType(): Type = raw
            override fun getActualTypeArguments(): Array<out Type> = args
            override fun getOwnerType(): Type? = null
        }
        return gson.fromJson(json, type)
    }
}

/**
 * 跳过 Kotlin `by lazy` 委托字段的 Gson 排除策略。
 *
 * 命名兜底：合成委托字段统一以 `$delegate` 结尾；
 * 类型兜底：极少数情况下字段名被混淆/重命名时再看类型。
 */
object LazyDelegateExclusion : ExclusionStrategy {
    override fun shouldSkipField(f: FieldAttributes): Boolean {
        if (f.name.endsWith("\$delegate")) return true
        return kotlin.Lazy::class.java.isAssignableFrom(f.declaredClass)
    }

    override fun shouldSkipClass(clazz: Class<*>): Boolean = false
}