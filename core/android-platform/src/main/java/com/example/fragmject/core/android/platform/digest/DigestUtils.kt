package com.example.fragmject.core.android.platform.digest

import java.security.MessageDigest

/**
 * 摘要计算：提供缓存键等场景需要的十六进制哈希。
 *
 * 收敛到本模块，避免各模块为取一个 md5 而引入 okhttp/okio 之类的传递依赖。
 *
 * 调用点位于 WebView 资源拦截的热路径（每个可缓存资源一次、每个主文档一次），
 * 因此做了两处优化：
 * - [MD5] 用 ThreadLocal 复用实例：`MessageDigest.getInstance` 每次都要做 Provider
 *   查找且带同步开销；
 * - 十六进制编码用查表 + `CharArray` 直接构造，替代 `toString(16).padStart(2, '0')`
 *   ——后者每个字节都会产生 `toString`、`padStart` 与 StringBuilder 追加等多份临时对象。
 */
object DigestUtils {

    private val HEX_CHARS = charArrayOf(
        '0', '1', '2', '3', '4', '5', '6', '7',
        '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
    )

    /** MD5 不是线程安全的，按线程复用；digest() 内部会自动 reset，无需手动重置。 */
    private val MD5: ThreadLocal<MessageDigest> = object : ThreadLocal<MessageDigest>() {
        override fun initialValue(): MessageDigest = MessageDigest.getInstance("MD5")
    }

    /** 计算 [raw] 的 MD5 十六进制串（小写，32 位）。 */
    fun md5Hex(raw: String): String {
        // ThreadLocal.initialValue 已保证非空，此处用 checkNotNull 表达契约（避免 !! 写法）
        val md5 = checkNotNull(MD5.get()) { "MD5 ThreadLocal 未初始化" }
        val bytes = md5.digest(raw.toByteArray(Charsets.UTF_8))
        val out = CharArray(bytes.size * 2)
        var i = 0
        for (b in bytes) {
            val v = b.toInt() and 0xFF
            out[i++] = HEX_CHARS[v ushr 4]
            out[i++] = HEX_CHARS[v and 0x0F]
        }
        return String(out)
    }
}
