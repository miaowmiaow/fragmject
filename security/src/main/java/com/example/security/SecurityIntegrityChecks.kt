package com.example.security

import android.util.Log
import java.io.File
import java.util.Locale

/**
 * 完整性校验类检测项：APK 签名、自身 so 的 `.text` CRC、资源文件摘要。
 *
 * 依赖全部经 [SecurityFacts] / [NativeChecks] 注入，可在纯 JVM 单测中验证：
 * 签名匹配（含冒号/大小写差异）、资源基线、so 缺失降级等分支。
 */

/**
 * APK 签名校验。
 *
 * 未配置 [SecurityConfig.expectedSignatureSha256] 时直接放行：未配置就强行校验，
 * 会把所有正常安装包误杀。
 *
 * 启用后如何判定：
 * - Android 9+（API 28）用 `signingInfo.apkContentsSigners`（覆盖 v2/v3 签名）；
 * - 更低版本回退到 `PackageInfo.signatures`（v1）；
 * - **任一**签名证书的 SHA-256 与期望集合中任一项一致即通过（支持多签名 / 渠道重签）。
 *
 * 局限：只比对「内容签名」，不能防御「用同一证书重新签名」的改包。
 *
 * @return true=通过或未启用；false=校验失败（调用方据此阻断）。
 */
internal fun verifySignature(facts: SecurityFacts, config: SecurityConfig): Boolean {
    if (config.expectedSignatureSha256.isEmpty()) return true

    val expected = config.expectedSignatureSha256.map { it.normalizeSha256() }.toSet()
    val actual = facts.packageSignatureSha256()
    if (actual.isEmpty()) {
        // 查询失败时判失败：宁可误杀也不放行；若线上出现误杀，可改为「失败放行 + 上报」
        return false
    }
    return actual.any { it.normalizeSha256() in expected }
}

/**
 * 自身 so 完整性校验。
 *
 * 流程：
 * 1. 用 [extractSelfSo] 从 APK 解压 `lib/<abi>/libappsecurity.so` 到私有目录；
 * 2. 交给 native 比对「磁盘 .text」与「内存 .text」的 CRC32。
 *
 * 失败降级原则：
 * - native 未加载 → 跳过（无从校验）；
 * - 解压失败（APK 结构异常/无该 ABI）→ 跳过，不误杀；
 * - native 返回 -1（无法校验）→ 跳过；只有返回 1（不一致）才判定失败。
 */
internal fun verifySelfSoIntegrity(facts: SecurityFacts, native: NativeChecks): Boolean {
    if (!native.available) return true
    val soFile = extractSelfSo(facts) ?: return true
    return native.verifySelfSoTextCrc(soFile.absolutePath) != CRC_MISMATCH
}

/**
 * 自身 so 的 `.rodata` 校验（H3 新增的第二道完整性检查）。
 *
 * 为什么单独一项：特征串、白名单、阈值都在 `.rodata` 里。只校验 `.text` 的话，
 * 攻击者可以不动代码、只改常量表让检测「合法地」失效（例如把阈值改成极大值）。
 *
 * 与 [verifySelfSoIntegrity] 完全独立：两者各判各的，任一处不一致即阻断，
 * 避免「patch 一处校验点即可全绕过」。
 */
internal fun verifySelfSoDataIntegrity(facts: SecurityFacts, native: NativeChecks): Boolean {
    if (!native.available) return true
    val soFile = extractSelfSo(facts) ?: return true
    return native.verifySelfSoDataCrc(soFile.absolutePath) != CRC_MISMATCH
}

/**
 * 从 APK 中解压自身 so 到应用缓存目录，并用「APK 路径 + 最后更新时间」做缓存。
 *
 * 为什么用缓存：每次启动解压数 MB 的 so 是浪费；只要 APK 没升级，解压结果就有效。
 * 为什么用 cacheDir：语义更明确，且系统清理后下次会自动重新解压。
 *
 * @return 解压后的文件；无法解压时返回 null（调用方跳过校验）。
 */
internal fun extractSelfSo(facts: SecurityFacts): File? {
    val abi = facts.supportedAbi() ?: return null
    val entryName = "lib/$abi/$SELF_SO_NAME"
    val target = File(facts.cacheDirPath(), SELF_SO_NAME)
    val keyFile = File(facts.cacheDirPath(), SELF_SO_KEY_FILE)

    val cacheKey = "${facts.apkPath()}:${facts.lastUpdateTimeMillis()}"
    if (target.exists() && keyFile.exists() && keyFile.readText() == cacheKey) {
        return target
    }

    if (!facts.copyApkEntry(entryName, target.absolutePath)) return null
    return try {
        keyFile.writeText(cacheKey)
        target
    } catch (e: Exception) {
        Log.w(SecurityGuard.TAG, "write so cache key failed", e)
        target
    }
}

/**
 * 关键资源完整性校验。
 *
 * 默认不启用（[SecurityConfig.expectedResourceSha256] 为空直接返回 true）。
 * 说明：APK 的 v2/v3 签名已经覆盖整个包（含资源），签名校验通过时，
 * 资源完整性在密码学上已被保证；本方法是「签名校验被绕过」时的二次兜底。
 */
internal fun verifyResourceIntegrity(facts: SecurityFacts, config: SecurityConfig): Boolean {
    if (config.expectedResourceSha256.isEmpty()) return true

    return config.expectedResourceSha256.all { (entryName, expected) ->
        val actual = facts.apkEntrySha256(entryName) ?: return false
        actual.normalizeSha256() == expected.normalizeSha256()
    }
}

/** 去掉冒号/空格并统一大写：兼容 `keytool` 输出格式与手写配置。 */
private fun String.normalizeSha256(): String =
    replace(":", "").trim().uppercase(Locale.US)

/** 与 CMake 产物名一致（`libappsecurity.so`）。 */
private const val SELF_SO_NAME = "libappsecurity.so"

/** so 解压缓存键文件名。 */
private const val SELF_SO_KEY_FILE = "libappsecurity.key"

/** `verifySelfSoTextCrc` 返回 1 表示「磁盘与内存不一致」。 */
private const val CRC_MISMATCH = 1
