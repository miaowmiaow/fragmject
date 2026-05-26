package com.example.miaow.base.http

import android.util.Log
import com.example.miaow.base.provider.BaseContentProvider
import com.example.miaow.base.utils.AppScope
import com.example.miaow.base.utils.FileUtil
import com.example.miaow.base.utils.GSonUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

/**
 * 接口响应的磁盘 JSON 缓存。
 *
 * 设计动机：WanAndroid 服务器不返回标准 HTTP Cache 头，OkHttp 的磁盘缓存形同虚设。
 * 这里在业务层做一层"按 cacheKey 落盘 JSON"的轻量缓存，配合 [com.example.fragment.project.data.repository]
 * 中的 SWR 算子，实现"先缓存上屏，再网络刷新"的离线友好体验。
 *
 * 取舍：
 * 1. 仅缓存 GET 类的成功响应（由调用方判定 errorCode == "0" 后写入），写操作永不进入；
 * 2. 缓存粒度由 cacheKey 决定（接口 url + 关键参数），不跟随登录态——登录相关接口一律不接入；
 * 3. 文件名用 sha1，避免特殊字符与跨平台路径长度问题；
 * 4. 默认 7 天 TTL，避免脏数据无限堆积，过期后视作未命中并被惰性删除；
 * 5. 容量上限：超过 [MAX_FILES] 个文件时按 mtime 升序淘汰最旧文件，避免缓存目录无限增长；
 * 6. 写盘原子化：先写 .tmp 再 renameTo，崩溃 / 进程被杀也不会留下半截文件污染下次反序列化。
 */
object ResponseCache {

    private const val TAG = "ResponseCache"
    private const val DIR = "api-cache"
    private const val TMP_SUFFIX = ".tmp"

    /** 缓存文件数量上限：超过即按 mtime 升序删除最旧 */
    private const val MAX_FILES: Int = 200

    /**
     * 读端反序列化用的 Gson：复用全局单例的 lazy-aware 版本，避免重复创建。
     * 它跳过 Kotlin `by lazy` 委托属性合成的 `xxx$delegate` 字段。
     */
    private val gson get() = GSonUtils.lazyAwareGson

    /** 默认缓存有效期：7 天 */
    const val DEFAULT_TTL_MILLIS: Long = 7L * 24 * 60 * 60 * 1000

    private val cacheDir: File by lazy {
        File(BaseContentProvider.context().cacheDir, DIR).apply {
            if (!exists()) {
                mkdirs()
            }
        }
    }

    /** 写盘互斥：保证同 key 的并发写不会相互覆盖中间状态 */
    private val writeMutex = Mutex()

    /**
     * 读取并反序列化缓存。命中且未过期返回对象，否则返回 null。
     * 过期文件会被惰性删除，避免磁盘累积。
     */
    suspend fun <T : HttpResponse> read(
        cacheKey: String,
        type: Class<T>,
        ttlMillis: Long = DEFAULT_TTL_MILLIS,
    ): T? = withContext(Dispatchers.IO) {
        val file = fileOf(cacheKey)
        if (!file.exists()) {
            return@withContext null
        }
        // 过期：删除并返回 null
        if (ttlMillis > 0 && System.currentTimeMillis() - file.lastModified() > ttlMillis) {
            runCatching { file.delete() }
            return@withContext null
        }
        try {
            val json = FileUtil.readFileBytes(file)?.toString(Charsets.UTF_8).orEmpty()
            if (json.isBlank()) {
                return@withContext null
            }
            gson.fromJson(json, type)
        } catch (e: Exception) {
            Log.e(TAG, "read failed: key=$cacheKey", e)
            // 读失败的文件可能已损坏，主动清理
            runCatching { file.delete() }
            null
        }
    }

    /**
     * 异步写入缓存。调用方传原始 JSON 字符串即可，不会阻塞业务协程。
     * 仅当 [json] 非空时才会落盘。
     *
     * 写盘流程：先写到 .tmp，写入完成后 renameTo 目标文件 → 即便中途 crash 也不会出现半截文件。
     * 写入完成后顺手做一次 LRU 淘汰，超额时按 mtime 升序删除最旧文件。
     */
    fun writeAsync(cacheKey: String, json: String) {
        if (json.isBlank()) return
        AppScope.launch {
            writeMutex.withLock {
                writeAtomically(cacheKey, json)
                trimIfExceeded()
            }
        }
    }

    /**
     * 清空全部接口缓存。用于"清理缓存"入口或登出时主动调用。
     */
    fun clearAll() {
        AppScope.launch {
            runCatching {
                cacheDir.listFiles()?.forEach { it.delete() }
            }
        }
    }

    private suspend fun writeAtomically(cacheKey: String, json: String) = withContext(Dispatchers.IO) {
        val target = fileOf(cacheKey)
        val tmp = File(target.parentFile, target.name + TMP_SUFFIX)
        try {
            FileUtil.writeToFile(
                content = json,
                charset = Charsets.UTF_8,
                destFile = tmp,
                append = false,
            )
            // renameTo 在多数文件系统上是原子的；失败则降级为先删后改名
            if (target.exists()) {
                runCatching { target.delete() }
            }
            if (!tmp.renameTo(target)) {
                Log.w(TAG, "rename tmp -> target failed, fallback to copy: key=$cacheKey")
                FileUtil.writeToFile(
                    content = json,
                    charset = Charsets.UTF_8,
                    destFile = target,
                    append = false,
                )
                runCatching { tmp.delete() }
            }
        } catch (e: Exception) {
            Log.e(TAG, "write failed: key=$cacheKey", e)
            runCatching { tmp.delete() }
        }
    }

    /**
     * LRU 风格淘汰：仅按文件数量做上限。考虑到接口 JSON 通常 <100KB，
     * 200 个文件 ≈ 20MB 量级，对手机存储友好；同时避免每次写盘都做尺寸求和的开销。
     */
    private suspend fun trimIfExceeded() = withContext(Dispatchers.IO) {
        val files = cacheDir.listFiles()
            ?.filter { it.isFile && !it.name.endsWith(TMP_SUFFIX) }
            ?: return@withContext
        if (files.size <= MAX_FILES) return@withContext
        // 旧文件优先淘汰：按 lastModified 升序
        val toDelete = files.sortedBy { it.lastModified() }
            .take(files.size - MAX_FILES)
        toDelete.forEach { runCatching { it.delete() } }
    }

    private fun fileOf(cacheKey: String): File = File(cacheDir, sha1(cacheKey))

    private fun sha1(input: String): String {
        return try {
            val md = MessageDigest.getInstance("SHA-1")
            val bytes = md.digest(input.toByteArray(Charsets.UTF_8))
            buildString(bytes.size * 2) {
                bytes.forEach { append(String.format("%02x", it)) }
            }
        } catch (e: Exception) {
            // 退化方案：极端情况下 MD 不可用时，用 hashCode 兜底
            input.hashCode().toString()
        }
    }
}
