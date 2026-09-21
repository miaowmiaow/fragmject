package com.example.fragmject.core.network.http

import com.google.gson.Gson
import com.google.gson.TypeAdapter
import com.google.gson.TypeAdapterFactory
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.full.memberProperties

/**
 * 修复「Gson 反序列化 Kotlin 类时非空 String 字段被置为 null」的问题。
 *
 * Gson 默认用 Unsafe 分配对象（不经过 Kotlin 构造器），因此 JSON 中缺失的字段
 * 不会被赋上 Kotlin 声明的默认值（如 `= ""`），而是保持 Java 默认值 `null`。
 * 平时展示层多有兜底不报错；但一旦对象参与 hashCode/equals 计算——典型如 Paging 3 的
 * `PageFetcherSnapshotState.insert` 会对 `PagingSource.LoadResult.Page` 计算 hashCode，
 * 进而逐字段调用 data class 的 hashCode——就会触发 `String.hashCode()` 的空指针。
 *
 * 本工厂**无需维护白名单**：借助 kotlin-reflect 的 [KProperty.returnType.isMarkedNullable]
 * 精确区分字段声明的可空性——
 *  - `val title: String` → 非空声明 → 反序列化后若为 null 则回填 `""`；
 *  - `val username: String?` → 可空声明 → 跳过，保留 null 语义（如 `token == null` 判断）。
 * 因此新增任何模型都会自动生效，也不会误伤可空字段。
 *
 * 仅服务网络层：服务端响应字段可能缺失，需将缺失的非空 String 回填空串。
 */
object NullSafeStringTypeAdapterFactory : TypeAdapterFactory {

    /** 缓存每个类需要归一化的「声明非空 String」字段，避免每次 read 都反射。 */
    private val cache = ConcurrentHashMap<Class<*>, List<Field>>()

    override fun <T> create(gson: Gson, type: TypeToken<T>): TypeAdapter<T>? {
        val rawType = type.rawType
        val fields = cache.getOrPut(rawType) { findNonNullStringFields(rawType) }
        if (fields.isEmpty()) return null

        val delegate = gson.getDelegateAdapter(this, type)
        return object : TypeAdapter<T>() {
            override fun write(out: JsonWriter, value: T) = delegate.write(out, value)

            override fun read(reader: JsonReader): T {
                val value = delegate.read(reader)
                fillNullStrings(value, fields)
                return value
            }
        }
    }

    private fun fillNullStrings(value: Any?, fields: List<Field>) {
        if (value == null) return
        fields.forEach { field ->
            try {
                if (field.get(value) == null) {
                    field.set(value, "")
                }
            } catch (_: Exception) {
                // 反射失败时保持原样，交由展示层兜底
            }
        }
    }

    /**
     * 找出「声明为非空 String」的实例字段（含继承自超类的字段）。
     *
     * 属性名来自 Kotlin 反射的 [memberProperties]（会包含继承属性），
     * 再与 Java 反射的 [Field]（含超类声明字段）按名称匹配，从而定位可写字段。
     */
    private fun findNonNullStringFields(rawType: Class<*>): List<Field> {
        val nonNullStringNames = rawType.kotlin.memberProperties
            .filter { it.returnType.classifier == String::class }
            .filter { !it.returnType.isMarkedNullable }
            .map { it.name }
            .toSet()

        val result = mutableListOf<Field>()
        var current: Class<*>? = rawType
        while (current != null && current != Any::class.java) {
            current.declaredFields
                .filter { !Modifier.isStatic(it.modifiers) }
                .filter { it.type == String::class.java }
                .filter { it.name in nonNullStringNames }
                .forEach { result += it }
            current = current.superclass
        }
        result.forEach { it.isAccessible = true }
        return result
    }
}
