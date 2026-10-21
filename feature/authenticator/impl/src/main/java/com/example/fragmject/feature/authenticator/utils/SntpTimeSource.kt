package com.example.fragmject.feature.authenticator.utils

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * SNTP（RFC 4330）对时，用于**时间漂移检测**（参考 HugeRivers/Authenticator 的 NtpTimeProvider）。
 *
 * TOTP 完全依赖设备时间：时间不准会生成「永远不对」的验证码，且用户无从排查。
 * 这里只做检测与提示，不改动系统时间——用户确认后自己去系统设置校准。
 *
 * 为什么放 feature 而不是 core：守卫规定 `core:android-platform` 仅 core 内部消费，
 * 而本检测只有验证器用；且它是纯 `java.net` 实现，不引入任何新依赖。
 * UDP 123 在部分网络下不通，查询失败一律静默返回 null（宁可不确定也不误报）。
 */
object SntpTimeSource {

    private const val NTP_PORT = 123
    private const val NTP_PACKET_SIZE = 48
    private const val TIMEOUT_MS = 3_000
    private const val NTP_HOST = "pool.ntp.org"

    /** NTP 纪元（1900）与 Unix 纪元（1970）之间的秒数差。 */
    private const val NTP_TO_UNIX_OFFSET_SECONDS = 2_208_988_800L

    /**
     * 查询 NTP 服务器，返回「设备时间 − 标准时间」的偏移毫秒数
     * （正数 = 设备慢了；负数 = 设备快了）。失败返回 null。
     */
    suspend fun queryOffsetMillis(): Long? = withContext(Dispatchers.IO) {
        runCatching {
            DatagramSocket().use { socket ->
                socket.soTimeout = TIMEOUT_MS
                val address = InetAddress.getByName(NTP_HOST)
                val packet = ByteArray(NTP_PACKET_SIZE).apply { this[0] = 0x1B }
                socket.send(DatagramPacket(packet, packet.size, address, NTP_PORT))
                val received = DatagramPacket(ByteArray(NTP_PACKET_SIZE), NTP_PACKET_SIZE)
                socket.receive(received)
                val buffer = received.data
                val secondsSince1900 = readUnsignedInt(buffer, 40)
                require(secondsSince1900 > 0L) { "NTP 服务器时间无效" }
                // 秒级精度足够判断 30s 量级的漂移，不必解析小数部分
                val unixMillis = (secondsSince1900 - NTP_TO_UNIX_OFFSET_SECONDS) * 1000L
                unixMillis - System.currentTimeMillis()
            }
        }.getOrNull()
    }

    private fun readUnsignedInt(buffer: ByteArray, offset: Int): Long =
        ((buffer[offset].toLong() and 0xFF) shl 24) or
            ((buffer[offset + 1].toLong() and 0xFF) shl 16) or
            ((buffer[offset + 2].toLong() and 0xFF) shl 8) or
            (buffer[offset + 3].toLong() and 0xFF)
}
