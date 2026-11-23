package com.example.fragmject.core.android.platform.log

/**
 * 日志脱敏：写进 logcat 的 URL 与文件路径不带查询串、签名与目录结构。
 *
 * ## 为什么不是「release 少打日志」
 *
 * 本项目安全关键路径统一用 `Log.e`：R8 的 `-assumenosideeffects` 会把 `Log.v/d/i/w`
 * 当无副作用裁掉（见加固文档 §6.1 第 9 条），线上排障正是靠这些日志。所以不能一删了之，
 * 要做的是**保留可诊断性、去掉敏感部分**：
 *
 * - URL 常见 `?token=...` / `&x-signature=...`（第三方图床、文章页都这样），
 *   而 logcat 会被 `adb bugreport` / 崩溃采集带走；
 * - 文件绝对路径带包名与 `/data/user/0/...` 结构，对定位问题没有额外价值。
 *
 * ## 用法
 *
 * - 外部 URL：`LogSanitizer.url(request.url.toString())`
 * - 本地文件：`LogSanitizer.path(file.absolutePath)`（只留文件名）
 * - 已经是文件名 / scheme 的（如 `$fileName`、`request.url.scheme`）不需要处理
 */
object LogSanitizer {

    /** 超长 URL 的截断阈值：保留可辨认的域名与前缀即可，避免一条日志刷满屏。 */
    private const val MAX_URL_LENGTH = 120

    private const val EMPTY = "-"

    /** 去掉 query（签名/token）与 fragment，并按 [MAX_URL_LENGTH] 截断。 */
    fun url(raw: String?): String {
        val value = raw?.trim()
        if (value.isNullOrEmpty()) return EMPTY
        val withoutFragment = value.substringBefore('#')
        val withoutQuery = withoutFragment.substringBefore('?')
        return withoutQuery.take(MAX_URL_LENGTH)
    }

    /** 只保留文件名，去掉目录（`/data/user/0/<pkg>/cache/...` 这类结构不进日志）。 */
    fun path(raw: String?): String {
        val value = raw?.trim()
        if (value.isNullOrEmpty()) return EMPTY
        return value.substringAfterLast('/')
    }
}
