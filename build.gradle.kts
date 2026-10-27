// Top-level build file. Convention plugins (build-logic) handle most subproject configuration.
// These apply-false declarations are required for the version catalog to resolve plugin versions.
plugins {
    base
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    // :benchmark 是 com.android.test 模块，只产出仪器测试 APK，不进生产包
    alias(libs.plugins.android.test) apply false
    // Baseline Profile 生成/合并插件（app 侧为 consumer，benchmark 侧为 producer）
    alias(libs.plugins.androidx.baselineprofile) apply false
    alias(libs.plugins.detekt)
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.parcelize) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.room3) apply false
    alias(libs.plugins.fragmject.android.stability.check)
    alias(libs.plugins.fragmject.android.dependency.guard)
}

// detekt 在根项目统一扫描所有模块的 main 源码（build-logic 是独立构建，不在此范围）。
// 只做纯语法分析、不接类型解析：够用来抓长方法/复杂度过高/坏味道，且不需要各模块先编译。
detekt {
    buildUponDefaultConfig = true
    parallel = true
    config.setFrom(layout.projectDirectory.file("config/detekt/detekt.yml"))
    // 不使用 baseline：存量问题已全部修掉，规则配置见 detekt.yml，
    // 新增违规一律阻断（基线文件会让"问题在哪"失去可见性）。
    source.setFrom(
        fileTree(rootDir) {
            // 必须用 `**`：Gradle 的 `*` 不跨路径分隔符，写成 `*/*/src/...` 会漏掉
            // feature/<域>/impl 这类三层模块（曾漏扫 18 个模块）
            include("**/src/main/java/**/*.kt", "**/src/main/kotlin/**/*.kt")
            exclude("**/build/**", "build-logic/**")
        },
    )
}

// 报告里统一使用相对路径：否则本地（/Users/...）与 CI（/home/runner/...）
// 的绝对路径不同，定位告警时要额外换算。
tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    basePath = rootDir.absolutePath
}

// 将架构守卫任务挂载到 check，确保本地/CI 全量构建时强制校验依赖方向与稳定性配置。
// 注：verifyModuleDependencies 声明了 notCompatibleWithConfigurationCache，
// 挂载后会使 check 与配置缓存不兼容；如遇缓存问题可改为在 CI 单独触发。
tasks.matching { it.name == "check" }.configureEach {
    dependsOn("verifyModuleDependencies", "verifyComposeStabilityConfig")
}

// ====== 单元测试覆盖率门槛（T-04）======
// 口径与 build-logic 中 unitTestCoverageReport 的 classDirectories 过滤一致：
// 只统计可在 JVM 单测执行的逻辑层，已排除 Compose UI 与 Hilt/注解处理器生成类。
// 使用前需先执行 `./gradlew unitTestCoverageReport`（CI 中两条命令按序执行）。
//
// 定标规则（2026-10-27 重校）：门槛 = 实测值 - 约 2pt 缓冲，**涨上去就抬**。
// 旧值（feature 50 / data-repository 55 / domain 80）是按"先能跑起来"拍的，
// 与实测水平（74.2% / 84.6%）脱节 19pt / 4.6pt，等于只保护了已经很好的模块。
// 每次重校把实测值记在门槛旁，避免下一次评审再问"为什么是这个数"。
tasks.register("verifyUnitTestCoverage") {
    group = "verification"
    description = "校验单元测试行覆盖率门槛（逻辑层口径）；需先执行 unitTestCoverageReport"

    // 在配置期取值并让 doLast 闭包捕获局部变量：脚本对象在任务执行时已不可用。
    val coverageRoot = rootDir

    doLast {
        fun lineRatio(moduleDir: File): Pair<Int, Int>? {
            val xml = File(
                moduleDir,
                "build/reports/jacoco/unitTestCoverageReport/unitTestCoverageReport.xml",
            )
            if (!xml.isFile) return null
            val factory = javax.xml.parsers.DocumentBuilderFactory.newInstance().apply {
                isNamespaceAware = false
                // JaCoCo 的 DOCTYPE 指向 report.dtd；不联网、不校验，直接忽略外部实体
                setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
            }
            val root = factory.newDocumentBuilder()
                .parse(xml)
                .documentElement
            var node: org.w3c.dom.Node? = root.firstChild
            while (node != null) {
                if (node is org.w3c.dom.Element &&
                    node.tagName == "counter" &&
                    node.getAttribute("type") == "LINE"
                ) {
                    return node.getAttribute("covered").toInt() to node.getAttribute("missed").toInt()
                }
                node = node.nextSibling
            }
            return null
        }

        fun check(label: String, covered: Int, missed: Int, gate: Double) {
            val total = covered + missed
            val pct = if (total == 0) 100.0 else covered * 100.0 / total
            logger.lifecycle("%-26s %5.1f%% (%d/%d)，门槛 %.0f%%".format(label, pct, covered, total, gate))
            if (pct < gate) {
                throw GradleException("覆盖率未达门槛：$label ${"%.1f".format(pct)}% < $gate%")
            }
        }

        val featureRoot = File(coverageRoot, "feature")
        var featureCovered = 0
        var featureMissed = 0
        var featureModules = 0
        featureRoot.listFiles()?.filter { it.isDirectory }?.sortedBy { it.name }?.forEach { feature ->
            val ratio = lineRatio(File(feature, "impl")) ?: return@forEach
            featureCovered += ratio.first
            featureMissed += ratio.second
            featureModules++
        }
        if (featureModules == 0) {
            throw GradleException("未找到 feature 覆盖率报告，请先执行 ./gradlew unitTestCoverageReport")
        }
        check("feature 逻辑层", featureCovered, featureMissed, gate = 50.0)

        lineRatio(File(coverageRoot, "core/data-repository"))
            ?.let { check("core:data-repository", it.first, it.second, gate = 65.0) }
            ?: throw GradleException("缺少 core:data-repository 覆盖率报告")

        lineRatio(File(coverageRoot, "core/domain"))
            ?.let { check("core:domain", it.first, it.second, gate = 82.0) }
            ?: throw GradleException("缺少 core:domain 覆盖率报告")

        // core:network 是协议与 HTTP 适配层，此前完全不在门槛覆盖范围内（实测 30.3%）。
        // 不设高门槛的原因：Retrofit 接口与拦截器的大部分分支需要真实服务端才能覆盖，
        // 强行抬高只会逼出"为覆盖率而测"的空测试。先纳入 28% 锁住现状、禁止倒退，
        // 后续补 interceptors / CookieJar 的 fake 驱动测试后再抬。
        lineRatio(File(coverageRoot, "core/network"))
            ?.let { check("core:network", it.first, it.second, gate = 28.0) }
            ?: throw GradleException("缺少 core:network 覆盖率报告")
    }
}