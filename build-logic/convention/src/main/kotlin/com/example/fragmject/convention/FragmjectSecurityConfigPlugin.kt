package com.example.fragmject.convention

import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.api.variant.BuildConfigField
import com.android.build.api.variant.LibraryAndroidComponentsExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import java.io.File
import java.security.KeyStore
import java.security.MessageDigest
import java.util.Properties

/**
 * `security` 库的宿主侧配置注入插件（`fragmject.security.config`）。
 *
 * ============================ 解决什么问题 ============================
 * 安全库的签名校验需要「构建期算出的证书 SHA-256」。之前这段逻辑写在
 * `app/build.gradle.kts` 里（约 40 行：读 keystore.properties → 探测 JKS/PKCS12 →
 * 取证书 → SHA-256 → buildConfigField）。对外交付后，**每个接入方都要抄一遍**，
 * 抄错一处（比如别名不对）就会静默关掉签名校验。
 *
 * 抽成插件后，接入方只需：
 *
 * ```kotlin
 * plugins { id("fragmject.security.config") }
 * ```
 *
 * 插件会读取根目录 `keystore.properties`（`storeFile` / `keyAlias` / `storePassword`），
 * 算出证书摘要并注入 `BuildConfig.EXPECTED_SIG_SHA256`。
 *
 * ============================ 失败语义 ============================
 * 取不到证书（无 keystore / 密码错误 / 别名不存在）时注入**空串**，构建继续：
 * 运行期 `SecurityConfig.expectedSignatureSha256` 为空即「不启用签名校验」。
 * 这样做的好处是本地无 keystore 的机器也能构建；风险是「以为开了其实没开」——
 * 因此插件会打一条 warning，且诊断模式（`diagnosticMode`）会在运行时显示 `signature=false`。
 *
 * ============================ 可覆盖项 ============================
 * - `-PsecurityCertSha256=<hex[,hex]>`：直接指定摘要（多渠道/外部传入时用），跳过度取；
 * - `-PsecurityKeystoreFile=<path>` / `-PsecurityKeyAlias=<alias>` / `-PsecurityStorePassword=<>`：
 *   覆盖 `keystore.properties` 中的对应项。
 */
class FragmjectSecurityConfigPlugin : Plugin<Project> {

    override fun apply(target: Project) = with(target) {
        val digest = resolveCertDigest()
        if (digest.isEmpty()) {
            logger.warn(
                "[security] 未能从 keystore 解析证书摘要，BuildConfig.EXPECTED_SIG_SHA256 将为空。" +
                    "运行期签名校验不会启用——生产包请检查 keystore.properties 或用 -PsecurityCertSha256 指定。",
            )
        } else {
            logger.lifecycle("[security] 签名证书摘要: $digest")
        }

        applyToApplication(digest)
        applyToLibrary(digest)
    }

    /** application 模块：把摘要写进所有变体的 BuildConfig。 */
    private fun Project.applyToApplication(digest: String) {
        extensions.findByType(ApplicationAndroidComponentsExtension::class.java) ?: return
        extensions.getByType(ApplicationAndroidComponentsExtension::class.java).onVariants { variant ->
            variant.buildConfigFields?.put(
                "EXPECTED_SIG_SHA256",
                BuildConfigField("String", "\"$digest\"", "安全库签名校验的期望证书摘要（构建期注入）"),
            )
        }
    }

    /** library 模块同样支持（有些宿主把安全配置放在自己的 library 里）。 */
    private fun Project.applyToLibrary(digest: String) {
        extensions.findByType(LibraryAndroidComponentsExtension::class.java) ?: return
        extensions.getByType(LibraryAndroidComponentsExtension::class.java).onVariants { variant ->
            variant.buildConfigFields?.put(
                "EXPECTED_SIG_SHA256",
                BuildConfigField("String", "\"$digest\"", "安全库签名校验的期望证书摘要（构建期注入）"),
            )
        }
    }

    /**
     * 解析证书摘要。
     *
     * 优先级：显式属性 → `keystore.properties`。解析失败返回空串（不阻断构建）。
     */
    private fun Project.resolveCertDigest(): String {
        val explicit = providers.gradleProperty("securityCertSha256").orNull
        if (!explicit.isNullOrBlank()) {
            return explicit.split(',').joinToString(",") { it.trim().uppercase() }
        }

        val propsFile = rootProject.file("keystore.properties")
        val props = Properties().apply {
            if (propsFile.exists()) propsFile.inputStream().use { load(it) }
        }

        val storePath = providers.gradleProperty("securityKeystoreFile").orNull
            ?: props.getProperty("storeFile")
            ?: return ""
        val alias = providers.gradleProperty("securityKeyAlias").orNull
            ?: props.getProperty("keyAlias")
            ?: return ""
        val storePassword = providers.gradleProperty("securityStorePassword").orNull
            ?: props.getProperty("storePassword")
            ?: return ""

        // storeFile 的写法各工程不一样：绝对路径、相对模块目录（本仓库是 `../keystore.key`）、
        // 相对仓库根。按「绝对 → 模块目录 → 仓库根」依次尝试，取第一个存在的。
        val raw = File(storePath)
        val store = when {
            raw.isAbsolute -> raw
            file(storePath).exists() -> file(storePath)
            else -> rootProject.file(storePath)
        }
        if (!store.exists()) return ""

        // 不假设扩展名：JKS / PKCS12 都试一遍
        val keyStore = listOf("JKS", "PKCS12").firstNotNullOfOrNull { type ->
            runCatching {
                KeyStore.getInstance(type).apply { load(store.inputStream(), storePassword.toCharArray()) }
            }.getOrNull()
        } ?: return ""

        val cert = runCatching { keyStore.getCertificate(alias) }.getOrNull() ?: return ""
        return MessageDigest.getInstance("SHA-256")
            .digest(cert.encoded)
            .joinToString("") { "%02X".format(it.toInt() and 0xFF) }
    }
}
