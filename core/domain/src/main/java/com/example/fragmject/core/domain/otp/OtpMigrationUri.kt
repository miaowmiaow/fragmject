package com.example.fragmject.core.domain.otp

/**
 * `otpauth-migration://` 批量导出格式（Google Authenticator「导出账户」二维码）。
 *
 * ```
 * otpauth-migration://offline?data={base64(protobuf MigrationPayload)}
 * ```
 *
 * 载荷是protobuf 编码（schema 为社区逆向的稳定格式）：
 *
 * ```
 * message MigrationPayload {
 *   message OtpParameters {
 *     bytes  secret    = 1;
 *     string name      = 2;  // "issuer:account"
 *     string issuer    = 3;
 *     uint32 algorithm = 4;  // 0/1=SHA1, 2=SHA256, 3=SHA512
 *     uint32 digits    = 5;  // 0/1=6 位, 2=8 位
 *     uint32 type      = 6;  // 1=HOTP, 2=TOTP
 *     uint32 counter   = 7;
 *   }
 *   repeated OtpParameters otp_parameters = 1;
 *   uint32 batch_size = 2;   // 分批导出：一个二维码只含一批，未含字段全部按缺省处理
 *   uint32 batch_index = 3;
 *   uint32 batch_id = 4;
 * }
 * ```
 *
 * **只做导入**，本应用导出仍用通用 `otpauth://`（其他验证器同样接受）。
 * 解析失败（scheme 不对、缺 data、base64/protobuf 损坏、账户缺 secret）返回空列表，
 * 由 UI 按「无法识别」提示，绝不静默导入半截数据。
 */
object OtpMigrationUri {

    private const val SCHEME = "otpauth-migration://"
    private const val HOST = "offline"

    /** data 参数在 query 中的键。 */
    private const val DATA_KEY = "data"

    fun parse(raw: String): List<OtpAccount> {
        if (!raw.startsWith(SCHEME, ignoreCase = true)) return emptyList()
        val host = raw.substring(SCHEME.length).substringBefore('/').substringBefore('?')
        if (!host.equals(HOST, ignoreCase = true)) return emptyList()
        val data = queryParam(raw.substringAfter('?', ""), DATA_KEY) ?: return emptyList()
        val payload = decodeBase64(data) ?: return emptyList()
        return runCatching { readMigration(payload).mapNotNull { it.toAccount() } }
            .getOrDefault(emptyList())
    }

    // ---- 编码（导出方向）----

    /**
     * 单张二维码能承载的 base64 字符数上限。
     *
     * 二维码容量越大，版本越高、模块越多；同样像素下每个模块越小就越难扫。
     * 取 600 字符（≈ v20@M，97 个模块），配合 300px 渲染约 3px/模块——
     * 这是「一张码装得下更多账户」与「另一台手机扫得动」之间的折中。
     * 宁可多出一张码，也不要生成一张密到扫不出来的码。
     */
    const val MAX_PAYLOAD_CHARS = 600

    /** 一个批次最多放多少个账户——防止极端长名字把一张码撑到扫不动。 */
    private const val MAX_ACCOUNTS_PER_BATCH = 20

    /** 批次字段（batch_size/index/id）的字节开销，估算容量时预留。 */
    private const val BATCH_FIELDS_BYTES = 12

    /**
     * 把账户编码为批量导出 URI；账户多时**自动分批**，返回每张二维码一个 URI。
     *
     * 分批是必须的：二维码容量有上限，超出就得切成多张，
     * 对方必须把每一批都扫完（漏一张就少一批账户），UI 需明确提示「第 x/y 张」。
     *
     * @param batchId 批次标识，同一批导出共用（默认取当前秒数）
     * @param maxPayloadChars 单张码的 base64 字符上限，见 [MAX_PAYLOAD_CHARS]
     */
    fun generate(
        accounts: List<OtpAccount>,
        batchId: Int = (System.currentTimeMillis() / 1000).toInt(),
        maxPayloadChars: Int = MAX_PAYLOAD_CHARS,
    ): List<String> {
        val encoded = accounts.mapNotNull { it.encodeParameters() }
        if (encoded.isEmpty()) return emptyList()

        val batches = chunkByCapacity(encoded, maxPayloadChars)
        return batches.mapIndexed { index, batch ->
            val payload = batch.fold(ByteArray(0)) { acc, bytes -> acc + lenDelimited(1, bytes) } +
                varintField(2, batches.size.toLong()) +
                varintField(3, index.toLong()) +
                varintField(4, batchId.toLong())
            "$SCHEME$HOST?data=" + java.util.Base64.getEncoder().encodeToString(payload)
        }
    }

    private fun chunkByCapacity(items: List<ByteArray>, maxChars: Int): List<List<ByteArray>> {
        val batches = ArrayList<List<ByteArray>>()
        var current = ArrayList<ByteArray>()
        var currentBytes = BATCH_FIELDS_BYTES
        for (item in items) {
            // 子消息的 tag + 长度 varint 开销按 3 字节估算
            val withItem = currentBytes + item.size + 3
            val tooBig = base64Length(withItem) > maxChars
            val tooMany = current.size >= MAX_ACCOUNTS_PER_BATCH
            if (current.isNotEmpty() && (tooBig || tooMany)) {
                batches += current
                current = ArrayList()
                currentBytes = BATCH_FIELDS_BYTES
            }
            current += item
            currentBytes += item.size + 3
        }
        if (current.isNotEmpty()) batches += current
        return batches
    }

    private fun base64Length(bytes: Int): Int = ((bytes + 2) / 3) * 4

    /** 单个账户 → OtpParameters 子消息；密钥不是合法 Base32 时无法编码，跳过。 */
    private fun OtpAccount.encodeParameters(): ByteArray? {
        val secretBytes = Base32.decode(secret) ?: return null
        if (accountName.isBlank()) return null
        val label = if (issuer.isNotBlank()) "$issuer:$accountName" else accountName
        return lenDelimited(1, secretBytes) +
            lenDelimited(2, label.toByteArray(Charsets.UTF_8)) +
            (if (issuer.isBlank()) ByteArray(0) else lenDelimited(3, issuer.toByteArray(Charsets.UTF_8))) +
            varintField(4, when (algorithm) {
                OtpAlgorithm.SHA256 -> 2L
                OtpAlgorithm.SHA512 -> 3L
                else -> 1L
            }) +
            varintField(5, if (digits == 8) 2L else 1L) +
            varintField(6, if (type == OtpType.HOTP) 1L else 2L) +
            varintField(7, counter)
    }

    private fun lenDelimited(field: Int, payload: ByteArray): ByteArray =
        varint(((field shl 3) or 2).toLong()) + varint(payload.size.toLong()) + payload

    private fun varintField(field: Int, value: Long): ByteArray =
        varint(((field shl 3) or 0).toLong()) + varint(value)

    private fun varint(value: Long): ByteArray {
        var v = value
        val out = ArrayList<Byte>()
        while (true) {
            val b = (v and 0x7F).toInt()
            v = v ushr 7
            if (v == 0L) {
                out.add(b.toByte())
                break
            }
            out.add((b or 0x80).toByte())
        }
        return out.toByteArray()
    }

    /** 取指定 query 参数并做百分号解码（`+` 是 base64 合法字符，必须原样保留）。 */
    private fun queryParam(query: String, key: String): String? {
        for (pair in query.split('&')) {
            if (pair.isEmpty()) continue
            val name = pair.substringBefore('=')
            if (name == key) {
                val value = pair.substringAfter('=', "")
                return OtpAuthUri.percentDecode(value).takeIf { it.isNotEmpty() }
            }
        }
        return null
    }

    /**
     * Base64 解码：先补齐 padding，再同时兼容标准与 URL-safe 字母表
     * （部分扫码实现会把 data 参数按 URL-safe 规则改写）。
     */
    private fun decodeBase64(value: String): ByteArray? = runCatching {
        val normalized = value.trim().replace('-', '+').replace('_', '/')
        val padded = normalized + "=".repeat((4 - normalized.length % 4) % 4)
        java.util.Base64.getMimeDecoder().decode(padded)
    }.getOrNull()

    // ---- protobuf 读取（仅覆盖本格式用到的 wire type，未知字段按类型跳过）----

    private const val WIRE_VARINT = 0
    private const val WIRE_FIXED64 = 1
    private const val WIRE_LEN = 2
    private const val WIRE_FIXED32 = 5

    private fun readMigration(bytes: ByteArray): List<OtpParameters> {
        val reader = ProtoReader(bytes)
        val result = ArrayList<OtpParameters>()
        while (reader.hasNext()) {
            val (field, wireType) = reader.readTag()
            if (field == 1 && wireType == WIRE_LEN) {
                result += readOtpParameters(reader.readLengthDelimited())
            } else {
                reader.skipValue(wireType)
            }
        }
        return result
    }

    private fun readOtpParameters(bytes: ByteArray): OtpParameters {
        var secret = ByteArray(0)
        var name = ""
        var issuer = ""
        var algorithm = 0L
        var digits = 0L
        var type = 0L
        var counter = 0L
        val reader = ProtoReader(bytes)
        while (reader.hasNext()) {
            val (field, wireType) = reader.readTag()
            when (field) {
                1 if wireType == WIRE_LEN -> secret = reader.readLengthDelimited()
                2 if wireType == WIRE_LEN -> name = reader.readString()
                3 if wireType == WIRE_LEN -> issuer = reader.readString()
                4 if wireType == WIRE_VARINT -> algorithm = reader.readVarint()
                5 if wireType == WIRE_VARINT -> digits = reader.readVarint()
                6 if wireType == WIRE_VARINT -> type = reader.readVarint()
                7 if wireType == WIRE_VARINT -> counter = reader.readVarint()
                else -> reader.skipValue(wireType)
            }
        }
        return OtpParameters(secret, name, issuer, algorithm, digits, type, counter)
    }

    /** 线上格式的单账户参数；枚举先保留原始数值，转换时再映射。 */
    private data class OtpParameters(
        val secret: ByteArray,
        val name: String,
        val issuer: String,
        val algorithm: Long,
        val digits: Long,
        val type: Long,
        val counter: Long,
    ) {
        // secret 为 ByteArray，data class 默认 equals/hashCode 采用引用相等，按内容重写。
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as OtpParameters

            if (!secret.contentEquals(other.secret)) return false
            if (name != other.name) return false
            if (issuer != other.issuer) return false
            if (algorithm != other.algorithm) return false
            if (digits != other.digits) return false
            if (type != other.type) return false
            if (counter != other.counter) return false

            return true
        }

        override fun hashCode(): Int {
            var result = secret.contentHashCode()
            result = 31 * result + name.hashCode()
            result = 31 * result + issuer.hashCode()
            result = 31 * result + algorithm.hashCode()
            result = 31 * result + digits.hashCode()
            result = 31 * result + type.hashCode()
            result = 31 * result + counter.hashCode()
            return result
        }

        fun toAccount(): OtpAccount? {
            if (secret.isEmpty()) return null

            // name 形如 "issuer:account"；无冒号时整段即账户名
            val colonIndex = name.indexOf(':')
            val labelIssuer = if (colonIndex > 0) name.substring(0, colonIndex) else null
            val accountName = if (colonIndex >= 0) name.substring(colonIndex + 1) else name
            if (accountName.isBlank()) return null

            return OtpAccount(
                issuer = issuer.ifBlank { labelIssuer.orEmpty() },
                accountName = accountName,
                secret = Base32.encode(secret),
                algorithm = when (algorithm) {
                    2L -> OtpAlgorithm.SHA256
                    3L -> OtpAlgorithm.SHA512
                    else -> OtpAlgorithm.SHA1
                },
                // 枚举是 1=SIX / 2=EIGHT，不是 6/8
                digits = if (digits == 2L) 8 else 6,
                type = if (type == 1L) OtpType.HOTP else OtpType.TOTP,
                counter = counter,
            )
        }
    }

    /** 极简 protobuf 读取器：只支持读，遇未知字段按 wire type 跳过。 */
    private class ProtoReader(private val bytes: ByteArray) {
        private var pos = 0

        fun hasNext(): Boolean = pos < bytes.size

        /** @return (fieldNumber, wireType) */
        fun readTag(): Pair<Int, Int> {
            val tag = readVarint()
            return (tag ushr 3).toInt() to (tag and 0x7).toInt()
        }

        fun readVarint(): Long {
            var result = 0L
            var shift = 0
            while (shift < 64) {
                require(pos < bytes.size) { "varint 越界" }
                val b = bytes[pos++].toInt() and 0xFF
                result = result or ((b and 0x7F).toLong() shl shift)
                if (b and 0x80 == 0) return result
                shift += 7
            }
            throw IllegalArgumentException("varint 超过 10 字节")
        }

        fun readLengthDelimited(): ByteArray {
            val length = readVarint().toInt()
            require(length >= 0 && pos + length <= bytes.size) { "长度越界" }
            val out = bytes.copyOfRange(pos, pos + length)
            pos += length
            return out
        }

        fun readString(): String = String(readLengthDelimited(), Charsets.UTF_8)

        fun skipValue(wireType: Int) {
            when (wireType) {
                WIRE_VARINT -> readVarint()
                WIRE_FIXED64 -> pos += 8
                WIRE_LEN -> readLengthDelimited()
                WIRE_FIXED32 -> pos += 4
                else -> throw IllegalArgumentException("不支持的 wire type $wireType")
            }
        }
    }
}
