import java.nio.file.Files
import java.util.zip.ZipFile

plugins {
    id("com.android.library")
}

/**
 * ============================ 配置解耦（对外交付的前提） ============================
 * 本库不再读宿主工程根目录的 `config.properties`：那是**某个应用**的构建配置，
 * 库被拷到别的工程后不一定存在、语义也未必一致。
 *
 * 现在版本号由库自己声明，需要临时覆盖时用 Gradle 属性（`-PsecurityMinSdk=23`），
 * 覆盖项只影响本模块，不会反向污染宿主配置。
 *
 * 数值来源：`compileSdk` 取「能编译通过的最高已知版本」，`minSdk` 由
 * `InMemoryDexClassLoader`（API 26）决定，NDK/CMake 取本仓库验证过的版本。
 */
val libCompileSdk: Int = providers.gradleProperty("securityCompileSdk").map(String::toInt).getOrElse(37)
val libMinSdk: Int = providers.gradleProperty("securityMinSdk").map(String::toInt).getOrElse(26)
val libNdkVersion: String = providers.gradleProperty("securityNdkVersion").getOrElse("28.2.13676358")
val libCmakeVersion: String = providers.gradleProperty("securityCmakeVersion").getOrElse("3.22.1")

/**
 * 运行期安全防护库（从 `:app` 搬出，供其他项目复用）。
 *
 * ============================ 为什么是顶层模块 ============================
 * 它不再属于某个应用的内部实现，而是要作为「一份能力」交付给其他项目，
 * 满足 `dependencies-policy.md` 的「物理隔离 / 独立交付」门槛；
 * 因此不放进 `:core`（core 天然依赖本项目上下文）。
 *
 * ============================ 对外契约 ============================
 * - 入口：`SecurityGuard.install(context, SecurityConfig(...))`（宿主在 attachBaseContext 调用）；
 * - 配置：`SecurityConfig` / `SecurityHit` / `Severity` / `Action`；
 * - native：随 AAR 分发 `libappsecurity.so`，JNI 走动态注册，宿主无需写任何 keep 规则
 *   （规则在 `consumer-rules.pro` 里，随 AAR 自动合并）。
 */
android {
    namespace = "com.example.security"
    // 库自身声明，不读宿主配置
    compileSdk = libCompileSdk

    // native 编译在这里发生；版本同样取自 config.properties，与 SDK 版本一起维护
    ndkVersion = libNdkVersion

    defaultConfig {
        // InMemoryDexClassLoader 需要 API 26+
        minSdk = libMinSdk
        // 宿主 R8 会重命名 JNI 类与公开 API，规则必须由库自带
        consumerProguardFiles("consumer-rules.pro")

        // 测试开关（只影响本模块的 native 编译）：
        // -PselfTamperProbe=true 时编译进「自篡改探针」，用于验证逐页 CRC 能定位被改的页。
        if (providers.gradleProperty("selfTamperProbe").map(String::toBoolean).getOrElse(false)) {
            externalNativeBuild {
                cmake {
                    arguments += "-DSELF_TAMPER_PROBE=ON"
                }
            }
        }
        // 测试开关：改写一个 GOT 表项，验证 PLT 劫持检测
        if (providers.gradleProperty("selfGotProbe").map(String::toBoolean).getOrElse(false)) {
            externalNativeBuild {
                cmake {
                    arguments += "-DSELF_GOT_PROBE=ON"
                }
            }
        }
        // 测试开关：把 .text 首页置为 RWX，验证「可写可执行映射」检测
        if (providers.gradleProperty("selfRwxProbe").map(String::toBoolean).getOrElse(false)) {
            externalNativeBuild {
                cmake {
                    arguments += "-DSELF_RWX_PROBE=ON"
                }
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            // 与本地安装版本保持一致，覆盖在 config.properties 的 cmakeVersion
            version = libCmakeVersion
        }
    }

    buildFeatures {
        // 库不需要 BuildConfig：所有决策由宿主的 SecurityConfig 注入
        buildConfig = false
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    // 库只依赖 Android 框架；检测项通过 SecurityFacts 抽象，可在纯 JVM 上测试
    testImplementation(libs.junit)
}

// ============================================================================================
// 公共 API 基线（对外交付后，破坏性改 API 必须在评审里显式可见）
//
// 为什么不用第三方插件：binary-compatibility-validator 需要联网拉依赖，而本仓库的
// 构建要能在离线环境跑通。这里用 JDK 自带的 javap 导出 public 签名，做文本基线对比，
// 足够抓住「删方法 / 改签名 / 改常量」这类真正的破坏，也不会引入版本管理负担。
//
// 用法：
//   ./gradlew :security:apiDump     # API 变更后更新基线（把 security/api/security.api 一起提交）
//   ./gradlew :security:apiCheck    # CI/本地校验：与基线不一致即失败
// ============================================================================================

/**
 * 用 object 而不是脚本顶层函数：Gradle 配置缓存下，`doLast` 里调用脚本函数会因为
 * 「脚本实例不可序列化」而报 `this$0 is null`（本任务实测踩到过）。
 */
object PublicApiDumper {
    fun dump(aar: File): String {
        val work = Files.createTempDirectory("security-api-").toFile()
        try {
            val classesJar = File(work, "classes.jar")
            ZipFile(aar).use { zip ->
                val entry = zip.getEntry("classes.jar") ?: error("AAR 内没有 classes.jar：$aar")
                zip.getInputStream(entry).use { input -> classesJar.outputStream().use { input.copyTo(it) } }
            }
            // 只导出**对外契约**的签名。
            //
            // 为什么用白名单而不是「排除 R 类」：Kotlin 的 `internal` 在字节码层面仍是 public，
            // javap 会把 NativeChecks / BuildValues 这类内部类型也当成 API —— 结果是
            // 内部实现一改基线就抖动，反而掩盖真正的破坏性变更。
            // 新增对外类型时必须同步这里（这本身就是一次「有意识公开」的动作）。
            val publicApiTypes = setOf(
                "com.example.security.SecurityGuard",
                "com.example.security.SecurityConfig",
                "com.example.security.SecurityHit",
                "com.example.security.SecurityEvent",
                "com.example.security.Severity",
                "com.example.security.Action",
                "com.example.security.SecurityCheck",
                "com.example.security.SecurityEventSink",
                "com.example.security.ThrottledEventSink",
                "com.example.security.FilteredEventSink",
            )
            val classNames = ZipFile(classesJar).use { zip ->
                zip.entries().asSequence()
                    .map { it.name }
                    .filter { it.endsWith(".class") }
                    .map { it.removeSuffix(".class").replace('/', '.') }
                    .filter { it in publicApiTypes }
                    .sorted()
                    .toList()
            }
            val javap = File(System.getProperty("java.home"), "bin/javap").absolutePath
            val builder = StringBuilder()
            classNames.forEach { name ->
                val process = ProcessBuilder(javap, "-public", "-classpath", classesJar.absolutePath, name)
                    .redirectErrorStream(true)
                    .start()
                val text = process.inputStream.bufferedReader().use { it.readText() }
                process.waitFor()
                builder.append(text.trim()).append('\n').append('\n')
            }
            return builder.toString().trim() + "\n"
        } finally {
            work.deleteRecursively()
        }
    }
}

val apiBaselineFile = layout.projectDirectory.file("api/security.api").asFile
val securityAarFile = layout.buildDirectory.file("outputs/aar/security-release.aar").get().asFile

val apiDump by tasks.registering {
    group = "verification"
    description = "导出 :security 的公共 API 基线（api/security.api）"
    dependsOn("assembleRelease")
    // 必须在这里就捕获成局部变量：`doLast` 里再访问脚本属性会报
    // `this$0 is null`（配置缓存下脚本实例不可用，实测踩到）
    val baseline = apiBaselineFile
    val aar = securityAarFile
    inputs.file(aar)
    outputs.file(baseline)
    doLast {
        baseline.parentFile.mkdirs()
        baseline.writeText(PublicApiDumper.dump(aar))
        logger.lifecycle("公共 API 基线已更新：${baseline.absolutePath}")
    }
}

val apiCheck by tasks.registering {
    group = "verification"
    description = "校验 :security 的公共 API 是否与基线一致"
    dependsOn("assembleRelease")
    val baseline = apiBaselineFile
    val aar = securityAarFile
    inputs.file(aar)
    inputs.file(baseline)
    doLast {
        check(baseline.exists()) {
            "缺少 API 基线：${baseline.absolutePath}；首次请先运行 :security:apiDump 并提交该文件。"
        }
        val actual = PublicApiDumper.dump(aar)
        if (actual.trim() != baseline.readText().trim()) {
            throw GradleException(
                "公共 API 与基线不一致（对外库的破坏性变更需要显式确认）。\n" +
                    "若是有意变更：运行 ./gradlew :security:apiDump 并提交 api/security.api。",
            )
        }
        logger.lifecycle("公共 API 与基线一致")
    }
}
