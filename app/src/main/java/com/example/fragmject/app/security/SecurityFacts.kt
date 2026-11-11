package com.example.fragmject.app.security

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Debug
import android.util.Log
import java.io.File
import java.net.InetSocketAddress
import java.net.Socket
import java.security.MessageDigest
import java.util.Locale
import java.util.zip.ZipFile

/**
 * 环境事实抽象：把「读系统/读文件/查包名」这类**有副作用且与设备相关**的操作收成一个接口。
 *
 * ============================ 为什么需要这层 ============================
 * 拆出前的检测项直接调用 `File(...).exists()`、`Build.MODEL`、`PackageManager`：
 * - 单测无法构造「有 su 文件」「模拟器指纹」「root 管理应用已安装」等场景，
 *   只能靠真机手工验证，回归成本极高；
 * - 设备相关的静态字段（`Build.*`）无法替换，导致检测逻辑与其取值强绑定。
 *
 * 抽出接口后，检测项只依赖本接口，单测里用假实现（见 `SecurityDetectorsTest`）即可穷举分支。
 * 生产实现是 [AndroidSecurityFacts]。
 */
internal interface SecurityFacts {

    /** 是否已连接 Java 调试器（`Debug.isDebuggerConnected()`）。 */
    fun isDebuggerConnected(): Boolean

    /** 是否在等待调试器（`Debug.waitingForDebugger()`）。 */
    fun waitingForDebugger(): Boolean

    /** 路径是否存在。 */
    fun exists(path: String): Boolean

    /** 读取文本；失败返回 null。 */
    fun readText(path: String): String?

    /** 设备 Build 特征快照。 */
    fun buildValues(): BuildValues

    /** 指定包名是否已安装。 */
    fun isPackageInstalled(pkg: String): Boolean

    /** 本机回环端口是否可连接。 */
    fun isPortOpen(port: Int, timeoutMs: Int): Boolean

    /** 本应用全部签名证书的 SHA-256（大写十六进制）；查询失败返回空列表。 */
    fun packageSignatureSha256(): List<String>

    /** 当前 APK 路径（`applicationInfo.sourceDir`）。 */
    fun apkPath(): String

    /** 应用私有缓存目录路径。 */
    fun cacheDirPath(): String

    /** 应用最后更新时间（毫秒），用于 APK 解压缓存失效判定。 */
    fun lastUpdateTimeMillis(): Long

    /** 首选 ABI（如 `arm64-v8a`）。 */
    fun supportedAbi(): String?

    /** 从当前 APK 中取指定 entry 写入 [targetPath]；entry 不存在或写入失败返回 false。 */
    fun copyApkEntry(entryName: String, targetPath: String): Boolean

    /** 当前 APK 内指定 entry 的 SHA-256（大写十六进制）；entry 不存在或读取失败返回 null。 */
    fun apkEntrySha256(entryName: String): String?
}

/** 设备 Build 特征快照（模拟器判定用）。 */
internal data class BuildValues(
    val fingerprint: String,
    val model: String,
    val product: String,
    val hardware: String,
    val brand: String,
    val manufacturer: String,
)

/**
 * 生产实现：直接用 Android 平台 API。
 *
 * 每个方法都做了边界兜底（失败返回空/默认值），因为它是所有检测项的公共依赖：
 * 单点抛异常会让整轮检测崩掉，而检测失败的正确语义是「放行」而不是「误杀」。
 */
internal class AndroidSecurityFacts(private val context: Context) : SecurityFacts {

    override fun isDebuggerConnected(): Boolean = Debug.isDebuggerConnected()

    override fun waitingForDebugger(): Boolean = Debug.waitingForDebugger()

    override fun exists(path: String): Boolean = try {
        File(path).exists()
    } catch (_: Exception) {
        false
    }

    override fun readText(path: String): String? = try {
        File(path).readText()
    } catch (_: Exception) {
        null
    }

    override fun buildValues(): BuildValues = BuildValues(
        fingerprint = Build.FINGERPRINT.orEmpty().lowercase(Locale.US),
        model = Build.MODEL.orEmpty().lowercase(Locale.US),
        product = Build.PRODUCT.orEmpty().lowercase(Locale.US),
        hardware = Build.HARDWARE.orEmpty().lowercase(Locale.US),
        brand = Build.BRAND.orEmpty().lowercase(Locale.US),
        manufacturer = Build.MANUFACTURER.orEmpty().lowercase(Locale.US),
    )

    override fun isPackageInstalled(pkg: String): Boolean = try {
        context.packageManager.getPackageInfo(pkg, 0)
        true
    } catch (_: PackageManager.NameNotFoundException) {
        false
    } catch (_: Exception) {
        false
    }

    override fun isPortOpen(port: Int, timeoutMs: Int): Boolean = try {
        Socket().use { socket ->
            socket.connect(InetSocketAddress("127.0.0.1", port), timeoutMs)
            true
        }
    } catch (_: Exception) {
        false
    }

    override fun packageSignatureSha256(): List<String> = try {
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
        signers?.map { digestHex(it.toByteArray(), "SHA256") }.orEmpty()
    } catch (e: Exception) {
        // 查询失败会让 verifySignature 判失败并阻断，必须留下线上可查的证据。
        Log.e(SecurityGuard.TAG, "query signature failed", e)
        emptyList()
    }

    override fun apkPath(): String = context.applicationInfo.sourceDir

    override fun cacheDirPath(): String = context.cacheDir.absolutePath

    override fun lastUpdateTimeMillis(): Long = try {
        context.packageManager.getPackageInfo(context.packageName, 0).lastUpdateTime
    } catch (_: Exception) {
        0L
    }

    override fun supportedAbi(): String? = Build.SUPPORTED_ABIS.firstOrNull()

    override fun copyApkEntry(entryName: String, targetPath: String): Boolean = try {
        writeApkEntry(apkPath(), entryName, targetPath)
    } catch (e: Exception) {
        Log.e(SecurityGuard.TAG, "copy apk entry failed: $entryName", e)
        false
    }

    /** 从 [apkPath] 取出 entry 写入 [targetPath]；entry 不存在返回 false。 */
    private fun writeApkEntry(apkPath: String, entryName: String, targetPath: String): Boolean {
        ZipFile(apkPath).use { zip ->
            val entry = zip.getEntry(entryName) ?: return false
            zip.getInputStream(entry).use { input ->
                File(targetPath).outputStream().use { output -> input.copyTo(output) }
            }
        }
        return true
    }

    override fun apkEntrySha256(entryName: String): String? = try {
        ZipFile(apkPath()).use { zip ->
            val entry = zip.getEntry(entryName) ?: return null
            zip.getInputStream(entry).use { input -> digestHex(input.readBytes(), "SHA256") }
        }
    } catch (e: Exception) {
        Log.w(SecurityGuard.TAG, "read apk entry failed: $entryName", e)
        null
    }
}

/**
 * 计算摘要并转为大写十六进制。
 *
 * `it.toInt() and 0xFF` 是必要的：Kotlin 的 Byte 是有符号的，
 * 直接格式化负数会得到 "-1" 之类的错误结果。
 */
internal fun digestHex(bytes: ByteArray, algorithm: String): String {
    val digest = MessageDigest.getInstance(algorithm)
    digest.update(bytes)
    return digest.digest().joinToString("") { "%02X".format(it.toInt() and 0xFF) }
}
