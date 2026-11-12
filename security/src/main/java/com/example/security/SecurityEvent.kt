package com.example.security

/**
 * 结构化安全事件：给**上报/风控**用的稳定载体。
 *
 * ============================ 为什么不直接用 SecurityHit ============================
 * [SecurityHit] 的 `reason` 是面向用户的**中文文案**，随时可能改（改文案不该影响上报口径）。
 * 对外交付后，宿主需要的是「稳定的机器可读标识」，因此拆出 [code]：
 *
 * | code | 含义 |
 * |---|---|
 * | `debugger.native` / `debugger.java` | 调试器（native TracerPid / JDWP） |
 * | `injection.frida_maps` / `injection.frida_thread` / `injection.frida_file` | Frida 特征 |
 * | `injection.anon_segment` | 匿名可执行内存段（参数见 [detail]） |
 * | `injection.port` | Frida Server 监听端口 |
 * | `integrity.libc_crc` / `integrity.self_so` / `integrity.self_so_data` | 完整性校验失败 |
 * | `integrity.signature` / `integrity.resource` | 签名 / 关键资源异常 |
 * | `tamper.debuggable` | 生产包被标记为可调试（改包） |
 * | `env.root` / `env.emulator` | 环境类信号（默认 WARN） |
 *
 * 约定：`code` 一旦发布就不再改；新增检测项只加新值。
 *
 * @param code 稳定标识（见上表）
 * @param reason 中文可读原因（给人看，可能变）
 * @param severity 级别，决定是否终止进程
 * @param timestampMillis 事件时间（`System.currentTimeMillis()`）
 * @param detail 结构化细节，例如匿名段数量与阈值；空表示无附加信息
 */
data class SecurityEvent(
    val code: String,
    val reason: String,
    val severity: Severity,
    val timestampMillis: Long,
    val detail: Map<String, String> = emptyMap(),
) {
    /**
     * 序列化为单行 JSON（手写，不引依赖）。
     *
     * 为什么库自己序列化：宿主的上报通道各不相同（埋点 SDK / 自建 HTTP / 本地落盘），
     * 让每个接入方各写一遍转义逻辑，早晚会在「原因里带引号」这种地方翻车。
     * 字段顺序固定（code/reason/severity/timestamp/detail），便于服务端按列解析。
     */
    fun toJson(): String = buildString(160) {
        append("{\"code\":\"")
        append(code.escapeJson())
        append("\",\"reason\":\"")
        append(reason.escapeJson())
        append("\",\"severity\":\"")
        append(severity.name)
        append("\",\"timestamp\":")
        append(timestampMillis)
        if (detail.isNotEmpty()) {
            append(",\"detail\":{")
            var first = true
            for ((key, value) in detail) {
                if (!first) append(',')
                first = false
                append('"').append(key.escapeJson()).append("\":\"")
                append(value.escapeJson()).append('"')
            }
            append('}')
        }
        append('}')
    }
}

/** JSON 字符串转义：只处理 JSON 必需的最小集合，控制字符统一转 \uXXXX。 */
private fun String.escapeJson(): String = buildString(length + 8) {
    for (ch in this@escapeJson) {
        when (ch) {
            '"' -> append("\\\"")
            '\\' -> append("\\\\")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            else -> if (ch < ' ') append("\\u%04x".format(ch.code)) else append(ch)
        }
    }
}
