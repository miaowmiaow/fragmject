package com.example.fragmject.convention

import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.api.variant.LibraryAndroidComponentsExtension
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.execution.TaskExecutionGraph
import java.io.File
import java.security.KeyStore
import java.security.MessageDigest

/**
 * `securityConfig { }` 扩展：宿主声明签名来源。
 *
 * 二选一：
 * - [certSha256]：直接给证书 SHA-256（hex，可含冒号，多个用逗号分隔）；
 * - [keystoreFile] + [keyAlias] + [keystorePassword]：从 keystore 计算。
 *
 * 密码不要写进仓库，推荐用 `-PsecurityStorePassword=...` 或 CI 环境变量传入。
 * 命令行 `-P` 属性优先级高于扩展配置，便于 CI 覆盖。
 */
open class FragmjectSecurityConfigExtension {
    var certSha256: String? = null
    var keystoreFile: String? = null
    var keyAlias: String? = null
    var keystorePassword: String? = null
}

/**
 * 宿主侧签名来源插件（`fragmject.security.config`）。
 *
 * ============================ 交付语义 ============================
 * 签名期望值**必须由使用方显式提供**，插件不再默认读取任何仓库内的 keystore：
 * - 没有配置签名来源时，非 debuggable 变体（release 等）**构建失败**；
 * - debuggable 变体没有配置时只告警，运行期签名校验不启用；
 * - 配置了来源但无法使用（文件不存在、密码错误、别名不存在、摘要格式错误）**一律失败**，
 *   不允许静默降级为空串——那会让「以为开了签名校验，其实没开」。
 *
 * 解析优先级：`-PsecurityCertSha256` → `-PsecurityKeystoreFile`（别名/密码同理）
 * → `securityConfig { certSha256 }` → `securityConfig { keystoreFile }`。
 *
 * ============================ 去向 ============================
 * 摘要**不再**注入 `BuildConfig`（那会把期望值变成 dex 常量，apktool 可改写）。
 * 需要它的模块（`:security`）通过 [resolveSecurityCertDigest] 取值，并以 CMake 宏编译进 native。
 */
class FragmjectSecurityConfigPlugin : Plugin<Project> {

    override fun apply(target: Project) = with(target) {
        val config = extensions.create("securityConfig", FragmjectSecurityConfigExtension::class.java)
        val digest: Lazy<String> = lazy { resolveSecurityCertDigest(config) }
        // 非 debuggable 变体名（配置阶段记录，供 task graph 阶段判断）
        val productionVariants = mutableSetOf<String>()
        collectProductionVariants(productionVariants)
        failIfProductionTaskWithoutDigest(digest, productionVariants)
    }

    /** application / library 模块：记录所有非 debuggable 变体，供构建期检查使用。 */
    private fun Project.collectProductionVariants(productionVariants: MutableSet<String>) {
        extensions.findByType(ApplicationAndroidComponentsExtension::class.java)?.onVariants { variant ->
            if (!variant.debuggable) productionVariants += variant.name
        }
        extensions.findByType(LibraryAndroidComponentsExtension::class.java)?.onVariants { variant ->
            if (!variant.debuggable) productionVariants += variant.name
        }
    }

    /**
     * 生产变体缺摘要时，**只在真正要构建它时**失败。
     *
     * 为什么不在配置阶段失败：Gradle 配置时会为所有变体跑 onVariants，若在那里 throw，
     * `assembleDebug` 也会被拦住——这是实测踩到的。这里改为 task graph 就绪后检查：
     * 图中包含该生产变体的任务（如 `assembleRelease`）才失败；debug 构建不受影响。
     */
    private fun Project.failIfProductionTaskWithoutDigest(
        digest: Lazy<String>,
        productionVariants: MutableSet<String>,
    ) {
        val projectPath = path
        gradle.taskGraph.whenReady {
            checkGraph(this, projectPath, digest, productionVariants)
        }
    }

    private fun checkGraph(
        graph: TaskExecutionGraph,
        projectPath: String,
        digest: Lazy<String>,
        productionVariants: Set<String>,
    ) {
        if (productionVariants.isEmpty() || digest.value.isNotEmpty()) return
        val hit = graph.allTasks.firstOrNull { task ->
            task.path.startsWith("$projectPath:") &&
                productionVariants.any { variant -> task.name.contains(variant.replaceFirstChar { it.uppercase() }) }
        } ?: return
        throw GradleException(
            "[security] 生产变体缺少签名期望值（任务 ${hit.path}），构建中止。\n" +
                "请在 build 脚本中配置：\n" +
                "  securityConfig { keystoreFile = \"...\"; keyAlias = \"...\" }\n" +
                "或命令行传入：-PsecurityCertSha256=<hex> / -PsecurityKeystoreFile=<path>。\n" +
                "签名校验是安全库的核心能力，不允许静默关闭。",
        )
    }
}

/**
 * 解析证书摘要，结果已归一化（大写 hex、无冒号、逗号分隔多个）。
 *
 * 未配置任何来源时返回空串（由插件的生产变体检查决定是否失败）；
 * 已配置但有任何错误都直接抛出，不返回空串。
 */
fun Project.resolveSecurityCertDigest(config: FragmjectSecurityConfigExtension): String {
    val explicitDigest = providers.gradleProperty("securityCertSha256").orNull
        ?: config.certSha256
    if (!explicitDigest.isNullOrBlank()) {
        return normalizeDigests(explicitDigest, "证书摘要（certSha256 / -PsecurityCertSha256）")
    }

    val storeFile = providers.gradleProperty("securityKeystoreFile").orNull
        ?: config.keystoreFile
        ?: run {
            logger.warn("[security] 未配置签名来源（securityConfig）：生产变体将无法构建，debug 变体运行期签名校验不启用。")
            return ""
        }
    val alias = providers.gradleProperty("securityKeyAlias").orNull
        ?: config.keyAlias
        ?: throw GradleException("[security] 配置了 keystore 但缺少 keyAlias（securityConfig.keyAlias / -PsecurityKeyAlias）")
    val password = providers.gradleProperty("securityStorePassword").orNull
        ?: config.keystorePassword
        ?: throw GradleException("[security] 配置了 keystore 但缺少密码（-PsecurityStorePassword，或 CI 环境变量）")

    return digestFromKeystore(storeFile, alias, password)
}

private fun Project.digestFromKeystore(storePath: String, alias: String, password: String): String {
    // storeFile 的写法各工程不一样：绝对路径、相对模块目录、相对仓库根。依次尝试。
    val raw = File(storePath)
    val store = when {
        raw.isAbsolute -> raw
        file(storePath).exists() -> file(storePath)
        else -> rootProject.file(storePath)
    }
    if (!store.exists()) {
        throw GradleException("[security] keystore 不存在：${store.absolutePath}")
    }

    // JKS / PKCS12 都试一遍，不假设扩展名
    val keyStore = listOf("JKS", "PKCS12").firstNotNullOfOrNull { type ->
        runCatching {
            KeyStore.getInstance(type).apply { store.inputStream().use { load(it, password.toCharArray()) } }
        }.getOrNull()
    } ?: throw GradleException("[security] 无法打开 keystore（密码错误或格式不支持）：${store.absolutePath}")

    val cert = keyStore.getCertificate(alias)
        ?: throw GradleException("[security] keystore 中不存在别名：$alias")

    return MessageDigest.getInstance("SHA-256")
        .digest(cert.encoded)
        .joinToString("") { "%02X".format(it.toInt() and 0xFF) }
}

/** 校验并归一化摘要字符串：每个都必须是 32 字节（64 位 hex）。 */
private fun Project.normalizeDigests(raw: String, source: String): String {
    val parts = raw.split(',')
        .map { it.replace(":", "").replace(" ", "").trim().uppercase() }
        .filter { it.isNotEmpty() }
    if (parts.isEmpty()) {
        throw GradleException("[security] $source 为空")
    }
    val bad = parts.firstOrNull { !SHA256_HEX.matches(it) }
    if (bad != null) {
        throw GradleException("[security] $source 格式错误（需要 64 位 hex，多个用逗号分隔）：$bad")
    }
    return parts.joinToString(",")
}

private val SHA256_HEX = Regex("^[0-9A-F]{64}$")
