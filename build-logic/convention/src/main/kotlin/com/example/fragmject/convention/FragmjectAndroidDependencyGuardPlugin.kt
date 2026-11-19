package com.example.fragmject.convention

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.artifacts.ProjectDependency
import java.io.File

/**
 * 模块依赖方向校验插件（id = "fragmject.android.dependency-guard"）。
 *
 * 在根项目注册 `verifyModuleDependencies` 任务：遍历所有子项目的
 * implementation / api 等配置中的 ProjectDependency，构建模块依赖图，
 * 将架构方向约束（feature → core 单向、impl 不跨依赖等）下沉到 Gradle 构建层。
 *
 * 补充 Konsist 架构测试的盲区：`implementation(project(...))` 不产生源码 import
 * 时 Konsist 无法感知，本任务基于 Gradle 依赖图可捕获。
 */
class FragmjectAndroidDependencyGuardPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        if (target != target.rootProject) return

        target.tasks.register("verifyModuleDependencies") {
            group = "verification"
            description =
                "校验模块间 Gradle 依赖方向，并禁止源码使用外部库却依赖传递路径（隐式依赖）"
            // 需遍历所有子项目的依赖图，无法与配置缓存兼容，始终重新执行
            notCompatibleWithConfigurationCache("遍历所有子项目的 Gradle 依赖图")

            doLast {
                // 执行期再构建依赖边，避免在配置阶段访问 Project 状态：
                // projectsEvaluated 回调是配置缓存不兼容的「逃逸点」，会导致缓存静默失效、每次全量重配。
                val edges = buildEdges(target)
                val violations = mutableListOf<String>().apply {
                    addAll(collectViolations(edges))
                    // 隐式依赖：源码 import 了某外部库，却未在本模块显式声明（靠别人的传递路径编译）
                    addAll(collectImplicitExternalDeps(target))
                }
                if (violations.isNotEmpty()) {
                    throw GradleException(
                        "模块依赖违规：\n" + violations.joinToString("\n") { "  - $it" }
                    )
                }
            }
        }
    }

    /** 配置期构建直接依赖边：模块路径 -> 依赖的模块路径集合（排除测试配置）。 */
    private fun buildEdges(root: Project): Map<String, Set<String>> {
        val edges = mutableMapOf<String, Set<String>>()
        for (sub in root.subprojects) {
            // 测试专用模块不在生产依赖图里：它只经 baselineProfile 配置被 generateBaselineProfile 驱动，
            // 且 com.android.test 会以 testedApk 形式反向指向 :app（会被环检测误判）
            if (sub.path in TEST_ONLY_MODULES) continue
            val projectDeps = mutableSetOf<String>()
            for (config in sub.configurations) {
                val name = config.name
                if (name.contains("test", ignoreCase = true) ||
                    name.contains("androidTest", ignoreCase = true)
                ) {
                    continue
                }
                for (dependency in config.allDependencies) {
                    if (dependency is ProjectDependency && dependency.path !in TEST_ONLY_MODULES) {
                        projectDeps.add(dependency.path)
                    }
                }
            }
            edges[sub.path] = projectDeps
        }
        return edges
    }

    private fun collectViolations(edges: Map<String, Set<String>>): List<String> {
        val violations = mutableListOf<String>()

        // 环检测：模块依赖图必须是无环 DAG，任何依赖环都属架构违规
        detectCycles(edges).forEach { cycle ->
            violations.add("检测到依赖环：$cycle")
        }

        // 新增：所有 feature:*:impl 必须被 app 组合根聚合，
        // 避免新增 feature 忘记聚合导致页面静默缺失
        val appDeps = edges[":app"].orEmpty()
        edges.keys
            .filter { it.startsWith(":feature:") && it.endsWith(":impl") }
            .filter { it !in appDeps }
            .forEach { impl ->
                violations.add(":app 未依赖 $impl（feature:*:impl 必须由 app 组合根聚合，否则页面静默缺失）")
            }

        for ((from, toSet) in edges) {
            for (to in toSet) {
                checkRule(from, to)?.let { violations.add(it) }
            }
        }
        return violations
    }

    /**
     * 隐式外部依赖检测：源码 import 了某外部库的符号，但本模块未显式声明对应构件。
     *
     * 背景：`implementation` 声明不会向下游传递。若模块 A 靠「A → B → core-ktx」编译，
     * 一旦 B 调整依赖（或改用 api/implementation），A 会在毫无征兆的情况下编译失败。
     * 本项目遵循「显式优于隐式」：凡在源码中使用的外部符号，其构件必须由本模块自己声明。
     *
     * 当前只覆盖 `androidx.core.*`：它是唯一实际出现过该问题的分组，且子包与构件的
     * 对应关系明确（绝大多数由 core-ktx / core 提供，splashscreen 是独立构件）。
     * 扩展到其他分组时，只需往 [CORE_ARTIFACT_BY_PACKAGE] 补映射。
     */
    private fun collectImplicitExternalDeps(root: Project): List<String> {
        val violations = mutableListOf<String>()
        for (sub in root.subprojects) {
            if (sub.path in TEST_ONLY_MODULES) continue
            val mainSrc = File(sub.projectDir, "src/main")
            if (!mainSrc.exists()) continue

            val usedPackages = mutableSetOf<String>()
            mainSrc.walkTopDown()
                .filter { it.isFile && it.extension == "kt" }
                .forEach { file ->
                    file.useLines { lines ->
                        lines.forEach { line ->
                            parseCoreImportPackage(line)?.let { usedPackages.add(it) }
                        }
                    }
                }
            if (usedPackages.isEmpty()) continue

            val declared = declaredAndroidxCoreArtifacts(sub)
            unmetCoreImports(usedPackages, declared).forEach { (pkg, required) ->
                violations.add(
                    "${sub.path} 源码使用 androidx.core.$pkg.*，但未显式声明 $required" +
                        "（禁止依赖传递路径：上游调整传递关系即编译失败）"
                )
            }
        }
        return violations
    }

    /** 本模块显式声明的 `androidx.core` 构件名集合（排除测试配置）。 */
    private fun declaredAndroidxCoreArtifacts(sub: Project): Set<String> {
        val names = mutableSetOf<String>()
        for (config in sub.configurations) {
            val name = config.name
            if (name.contains("test", ignoreCase = true) ||
                name.contains("androidTest", ignoreCase = true)
            ) {
                continue
            }
            for (dependency in config.allDependencies) {
                if (dependency is ExternalModuleDependency && dependency.group == "androidx.core") {
                    names.add(dependency.name)
                }
            }
        }
        return names
    }
}

/**
 * 只用于仪器测试、不进入生产依赖图的模块。
 *
 * `:benchmark` 是 `com.android.test` 模块：app 通过 `baselineProfile` 配置驱动它跑
 * `generateBaselineProfile`，它又通过 `targetProjectPath` 反向持有 :app 的 testedApk。
 * 这条边既不是运行期依赖，也会被环检测误判成 `:app -> :benchmark -> :app`，
 * 因此整条边（两个方向）都不参与方向校验。
 */
private val TEST_ONLY_MODULES: Set<String> = setOf(":benchmark")
