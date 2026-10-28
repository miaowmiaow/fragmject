package com.example.fragmject.app.security

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import com.example.fragmject.app.security.SecurityPolicy.EXPECTED_SIGNATURE_SHA256
import java.io.File
import java.security.MessageDigest
import java.util.zip.ZipFile

/**
 * 完整性校验类检测项：APK 签名、自身 so 的 `.text` CRC、资源文件 CRC。
 *
 * 全部以 `AppSecurityGuard` 的扩展函数提供，与 `SecurityDetectors.kt` 同包。
 * 这些项都需要 native 的 CRC 校验配合，so 未加载时会走降级路径（见各函数 KDoc）。
 */

    /**
     * APK 签名校验。
     *
     * 为什么默认关闭（[EXPECTED_SIGNATURE_SHA256] 为空）：未配置时校验必然失败，
     * 会把所有正常安装包误杀；签名摘要依赖具体 keystore，不适合在源码里硬编码。
     *
     * 启用后如何判定：
     * - Android 9+（API 28）用 `signingInfo.apkContentsSigners`（覆盖 v2/v3 签名）；
     * - 更低版本回退到 `PackageInfo.signatures`（v1）；
     * - 任一签名证书的 SHA-256 与预期一致即通过。
     *
     * 局限：只比对「内容签名」，不能防御「用同一证书重新签名」的改包；
     * 多签名 APK 需要逐一比对全部 signer，本项目未启用该场景。
     *
     * @return true=通过或未启用；false=校验失败（调用方据此阻断）。
     */
    internal fun AppSecurityGuard.verifySignature(context: Context): Boolean {
        if (EXPECTED_SIGNATURE_SHA256.isBlank()) return true

        return try {
            val pm = context.packageManager
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES)
            }

            val signers = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.signingInfo?.apkContentsSigners
            } else {
                @Suppress("DEPRECATION")
                packageInfo.signatures
            }

            signers?.any { cert ->
                digestHex(cert.toByteArray(), "SHA256")
                    .equals(EXPECTED_SIGNATURE_SHA256, ignoreCase = true)
            } == true
        } catch (e: Exception) {
            // 查询失败（如 PackageManager 异常）时返回 false：宁可误杀也不放行，
            // 若线上出现误杀，可改为「失败放行 + 上报」策略。
            Log.w(TAG, "signature check failed", e)
            false
        }
    }

    /**
     * 计算摘要并转为大写十六进制。
     *
     * `it.toInt() and 0xFF` 是必要的：Kotlin 的 Byte 是有符号的，
     * 直接格式化负数会得到 "-1" 之类的错误结果。
     */
    internal fun AppSecurityGuard.digestHex(bytes: ByteArray, algorithm: String): String {
        val digest = MessageDigest.getInstance(algorithm)
        digest.update(bytes)
        return digest.digest().joinToString("") { "%02X".format(it.toInt() and 0xFF) }
    }

    /**
     * 自身 so 完整性校验。
     *
     * 流程：
     * 1. 用 [extractSelfSo] 从 APK 解压 `lib/<abi>/libfragmject_security.so` 到私有目录；
     * 2. 交给 native 比对「磁盘 .text」与「内存 .text」的 CRC32。
     *
     * 失败降级原则：
     * - native 未加载 → 跳过（无从校验）；
     * - 解压失败（APK 结构异常/无该 ABI）→ 跳过，不误杀；
     * - native 返回 -1（无法校验）→ 跳过；只有返回 1（不一致）才判定失败。
     */
    internal fun AppSecurityGuard.verifySelfSoIntegrity(context: Context): Boolean {
        if (!nativeLoaded) return true
        return try {
            val soFile = extractSelfSo(context) ?: return true
            nativeVerifySelfSoTextCrc(soFile.absolutePath) != 1
        } catch (e: Exception) {
            Log.w(TAG, "self so integrity check failed", e)
            true
        }
    }

    /**
     * 从 APK 中解压自身 so 到 `cacheDir`，并用「APK 路径 + 最后更新时间」做缓存。
     *
     * 为什么用缓存：每次启动解压数 MB 的 so 是浪费；只要 APK 没升级，解压结果就有效。
     * 为什么用 cacheDir：语义更明确，且系统清理后下次会自动重新解压。
     *
     * @return 解压后的文件；无法解压时返回 null（调用方跳过校验）。
     */

    /** 从 APK 中取出指定 entry 写到 [target]；entry 不存在时返回 false。 */
    private fun copyEntryFromApk(apkPath: String, entryName: String, target: File): Boolean {
        ZipFile(apkPath).use { zip ->
            val entry = zip.getEntry(entryName) ?: return false
            zip.getInputStream(entry).use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            }
        }
        return true
    }

    internal fun AppSecurityGuard.extractSelfSo(context: Context): File? {
        val abi = Build.SUPPORTED_ABIS.firstOrNull() ?: return null
        val entryName = "lib/$abi/libfragmject_security.so"
        val apkPath = context.applicationInfo.sourceDir
        val target = File(context.cacheDir, "libfragmject_security.so")
        val keyFile = File(context.cacheDir, "libfragmject_security.key")

        // 用 APK 路径 + lastUpdateTime 作为缓存键，应用升级后自动失效。
        val cacheKey = try {
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            "$apkPath:${info.lastUpdateTime}"
        } catch (_: Exception) {
            apkPath
        }

        if (target.exists() && keyFile.exists() && keyFile.readText() == cacheKey) {
            return target
        }

        return try {
            if (!copyEntryFromApk(apkPath, entryName, target)) return null
            keyFile.writeText(cacheKey)
            target
        } catch (e: Exception) {
            Log.w(TAG, "extract self so failed", e)
            null
        }
    }

    /**
     * 关键资源完整性校验。
     *
     * 默认不启用（[EXPECTED_RESOURCE_SHA256] 为空直接返回 true）。
     * 说明：APK 的 v2/v3 签名已经覆盖整个包（含资源），签名校验通过时，
     * 资源完整性在密码学上已被保证；本方法是「签名校验被绕过」时的二次兜底。
     */
    internal fun AppSecurityGuard.verifyResourceIntegrity(context: Context): Boolean {
        if (EXPECTED_RESOURCE_SHA256.isEmpty()) return true

        return try {
            val apkPath = context.applicationInfo.sourceDir
            ZipFile(apkPath).use { zip ->
                EXPECTED_RESOURCE_SHA256.all { (entryName, expected) ->
                    val entry = zip.getEntry(entryName) ?: return false
                    val actual = zip.getInputStream(entry).use {
                        digestHex(it.readBytes(), "SHA256")
                    }
                    actual.equals(expected, ignoreCase = true)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "resource integrity check failed", e)
            false
        }
    }
