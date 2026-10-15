package com.example.fragmject.core.domain.otp

import java.io.ByteArrayOutputStream

/**
 * `otpauth://` URI：主流验证器通用的账户导入 / 导出格式。
 *
 * ```
 * otpauth://totp/{issuer}:{account}?secret={base32}&issuer={issuer}&algorithm={}&digits={}&period={}
 * otpauth://hotp/{issuer}:{account}?secret={base32}&counter={}
 * ```
 *
 * **解析失败一律返回 null**：URI 不合法、缺 secret、secret 非合法 Base32 或参数越界。
 * 宁可拒绝导入，也不用错误的默认值静默生成一个「验证码永远不对」的账户。
 */
data class OtpAuthUri(
    val type: OtpType,
    val secret: String,
    val accountName: String,
    val issuer: String? = null,
    val algorithm: OtpAlgorithm = OtpAlgorithm.SHA1,
    val digits: Int = 6,
    val period: Int = 30,
    val counter: Long = 0,
) {

    companion object {

        private const val SCHEME = "otpauth://"

        fun parse(raw: String): OtpAuthUri? {
            if (!raw.startsWith(SCHEME, ignoreCase = true)) return null
            val rest = raw.substring(SCHEME.length)
            val type = OtpType.fromWireValue(rest.substringBefore('/', "")) ?: return null

            val afterType = rest.substringAfter('/', "")
            val label = percentDecode(afterType.substringBefore('?'))
            val params = parseQuery(afterType.substringAfter('?', ""))

            val secret = params["secret"]
                ?.replace(" ", "")
                ?.uppercase()
                ?.takeIf { it.isNotEmpty() && Base32.isValid(it) }
                ?: return null

            // label 形如 "issuer:account"；无冒号时整段即账户名
            val colonIndex = label.indexOf(':')
            val labelIssuer = if (colonIndex > 0) label.substring(0, colonIndex) else null
            val accountName = if (colonIndex >= 0) label.substring(colonIndex + 1) else label
            if (accountName.isBlank()) return null

            val issuer = params["issuer"]?.takeIf { it.isNotBlank() }
                ?: labelIssuer?.takeIf { it.isNotBlank() }

            val digits = params["digits"]?.toIntOrNull() ?: 6
            if (digits !in 6..8) return null

            val period = params["period"]?.toIntOrNull() ?: 30
            if (period <= 0) return null

            val counter = params["counter"]?.toLongOrNull() ?: 0

            return OtpAuthUri(
                type = type,
                secret = secret,
                accountName = accountName,
                issuer = issuer,
                algorithm = OtpAlgorithm.fromWireValue(params["algorithm"]),
                digits = digits,
                period = period,
                counter = counter,
            )
        }

        /** 生成 URI；省略等于默认值的参数，与主流验证器输出保持一致。 */
        fun generate(uri: OtpAuthUri): String {
            val label = buildString {
                uri.issuer?.takeIf { it.isNotBlank() }?.let {
                    append(percentEncode(it)).append(':')
                }
                append(percentEncode(uri.accountName))
            }
            val params = buildList {
                add("secret=${uri.secret}")
                uri.issuer?.takeIf { it.isNotBlank() }?.let { add("issuer=${percentEncode(it)}") }
                if (uri.algorithm != OtpAlgorithm.SHA1) add("algorithm=${uri.algorithm.wireValue}")
                if (uri.digits != 6) add("digits=${uri.digits}")
                if (uri.type == OtpType.TOTP && uri.period != 30) add("period=${uri.period}")
                if (uri.type == OtpType.HOTP) add("counter=${uri.counter}")
            }
            return "$SCHEME${uri.type.wireValue}/$label?${params.joinToString("&")}"
        }

        private fun parseQuery(query: String): Map<String, String> {
            if (query.isEmpty()) return emptyMap()
            val result = HashMap<String, String>()
            for (pair in query.split('&')) {
                if (pair.isEmpty()) continue
                result[percentDecode(pair.substringBefore('='))] =
                    percentDecode(pair.substringAfter('=', ""))
            }
            return result
        }

        /**
         * 百分号解码（只处理 `%XX`），结果按 UTF-8 解释。
         *
         * 必须以**字节流**收集后再整体解码：`%E8%85%BE` 是「腾」的 UTF-8 三字节，
         * 逐字节转成 Char 会得到三个乱码字符（如 `è¾`），中文发行方名会显示错乱。
         *
         * 刻意不用 `URLDecoder`：它按表单语义把 `+` 解码为空格，
         * 而 URI 中的 `+` 应原样保留，否则带 `+` 的账户名会被改坏。
         */
        internal fun percentDecode(value: String): String {
            val out = ByteArrayOutputStream(value.length)
            var i = 0
            while (i < value.length) {
                if (value[i] == '%' && i + 2 < value.length) {
                    val code = value.substring(i + 1, i + 3).toIntOrNull(radix = 16)
                    if (code != null) {
                        out.write(code)
                        i += 3
                        continue
                    }
                }
                out.write(value[i].toString().toByteArray(Charsets.UTF_8))
                i++
            }
            return String(out.toByteArray(), Charsets.UTF_8)
        }

        /**
         * 百分号编码。保留 `@`（URI path 中的合法字符，主流验证器也不编码它），
         * 使生成的 URI 与 Google Authenticator 等输出形式一致。
         */
        private fun percentEncode(value: String): String {
            val safe = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_.~@"
            val out = StringBuilder(value.length)
            for (c in value) {
                if (c in safe) {
                    out.append(c)
                } else {
                    // 非 ASCII 按 UTF-8 逐字节编码
                    for (byte in c.toString().toByteArray(Charsets.UTF_8)) {
                        out.append('%')
                        out.append(
                            (byte.toInt() and 0xFF)
                                .toString(radix = 16)
                                .uppercase()
                                .padStart(2, '0'),
                        )
                    }
                }
            }
            return out.toString()
        }
    }
}
