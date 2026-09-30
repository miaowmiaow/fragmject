package com.example.fragmject.convention

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency

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

        // 配置期（所有子项目评估完成后）构建依赖边，执行期仅消费纯数据，
        // 避免在任务执行期访问 Project 状态（这是配置缓存不兼容的根源）。
        lateinit var edges: Map<String, Set<String>>
        target.gradle.projectsEvaluated {
            edges = buildEdges(target)
        }

        target.tasks.register("verifyModuleDependencies") {
            group = "verification"
            description = "校验模块间 Gradle 依赖方向，防止绕过源码级架构约束"
            // 需遍历所有子项目的依赖图，无法与配置缓存兼容，始终重新执行
            notCompatibleWithConfigurationCache("遍历所有子项目的 Gradle 依赖图")

            doLast {
                val violations = collectViolations(edges)
                if (violations.isNotEmpty()) {
                    throw GradleException(
                        "模块依赖方向违规：\n" + violations.joinToString("\n") { "  - $it" }
                    )
                }
            }
        }
    }

    /** 配置期构建直接依赖边：模块路径 -> 依赖的模块路径集合（排除测试配置）。 */
    private fun buildEdges(root: Project): Map<String, Set<String>> {
        val edges = mutableMapOf<String, Set<String>>()
        for (sub in root.subprojects) {
            val projectDeps = mutableSetOf<String>()
            for (config in sub.configurations) {
                val name = config.name
                if (name.contains("test", ignoreCase = true) ||
                    name.contains("androidTest", ignoreCase = true)
                ) {
                    continue
                }
                for (dependency in config.allDependencies) {
                    if (dependency is ProjectDependency) {
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

        for ((from, toSet) in edges) {
            for (to in toSet) {
                checkRule(from, to)?.let { violations.add(it) }
            }
        }
        return violations
    }

    private fun checkRule(from: String, to: String): String? {
        // 新增：app 组合根只允许依赖白名单内的模块（正向声明），
        // 防止引入不必要的聚合边（如仅 Hilt 聚合、无源码引用却显式依赖的模块）
        if (from == ":app" && to !in APP_ALLOWED_DEPENDENCIES) {
            return "$from -> $to（app 依赖超出组合根白名单）"
        }
        // R7: core / feature 不得依赖 :app
        if ((from.startsWith(":core:") || from.startsWith(":feature:")) && to == ":app") {
            return "$from -> $to（core/feature 不得依赖 :app）"
        }
        // R4: core 不得反向依赖 feature
        if (from.startsWith(":core:") && to.startsWith(":feature:")) {
            return "$from -> $to（core 不得反向依赖 feature）"
        }
        // R1: domain 不得依赖 data-contract / data-repository / network / database
        if (from == ":core:domain" && to in setOf(
                ":core:data-contract",
                ":core:data-repository",
                ":core:network",
                ":core:database",
            )
        ) {
            return "$from -> $to（domain 不得依赖 data/network/database）"
        }
        // R5: data-repository 不得依赖 network / database
        if (from == ":core:data-repository" && to in setOf(":core:network", ":core:database")) {
            return "$from -> $to（data-repository 不得依赖 network/database）"
        }
        // R6: feature:*:api 不得依赖任何 feature:*:impl
        if (from.startsWith(":feature:") && from.endsWith(":api") &&
            to.startsWith(":feature:") && to.endsWith(":impl")
        ) {
            return "$from -> $to（feature api 不得依赖 feature impl）"
        }
        // R2 / R3: feature:*:impl 不得依赖其他 feature 的 impl / api
        if (from.startsWith(":feature:") && from.endsWith(":impl")) {
            val fromFeature = from.removePrefix(":feature:").substringBefore(":")
            if (to.startsWith(":feature:") && to != from) {
                val toFeature = to.removePrefix(":feature:").substringBefore(":")
                if (toFeature != fromFeature) {
                    if (to.endsWith(":impl")) {
                        return "$from -> $to（feature impl 不得依赖其他 feature impl）"
                    }
                    if (to.endsWith(":api")) {
                        return "$from -> $to（feature impl 不得依赖其他 feature api）"
                    }
                }
            }
        }
        // 新增：core 模块内部依赖白名单（正向声明，未声明的 core→core 边一律失败）
        if (from.startsWith(":core:") && to.startsWith(":core:")) {
            val allowed = CORE_ALLOWED_DEPENDENCIES[from]
            if (allowed == null || to !in allowed) {
                return "$from -> $to（core 模块依赖超出白名单，仅允许依赖：$allowed）"
            }
        }
        return null
    }

    /**
     * 依赖环检测：对模块依赖图做 DFS，报告所有依赖环。
     * core 模块必须是无环 DAG，任何环都属架构违规。
     */
    private fun detectCycles(edges: Map<String, Set<String>>): List<String> {
        val cycles = mutableListOf<String>()
        val visiting = mutableSetOf<String>()
        val visited = mutableSetOf<String>()
        val stack = mutableListOf<String>()

        fun dfs(node: String) {
            if (node in visited || node in visiting) return
            visiting.add(node)
            stack.add(node)
            for (next in edges[node].orEmpty()) {
                if (next in visiting) {
                    val start = stack.indexOf(next)
                    val cycle = stack.subList(start, stack.size) + next
                    cycles.add(cycle.joinToString(" -> "))
                } else if (next !in visited) {
                    dfs(next)
                }
            }
            stack.removeAt(stack.size - 1)
            visiting.remove(node)
            visited.add(node)
        }

        for (node in edges.keys) {
            dfs(node)
        }
        return cycles.distinct()
    }
}

/**
 * app 组合根允许依赖的模块白名单（正向声明）。
 *
 * app 是唯一组合根：依赖全部 feature:*:impl 以注册其 NavContent/Hilt 入口，
 * 并依赖 Hilt 聚合所需的 core 模块（database/data-repository/network 等）。
 * 未声明的 app→x 边一律失败，防止引入不必要的聚合依赖。
 */
private val APP_ALLOWED_DEPENDENCIES: Set<String> = setOf(
    ":core:android-platform",
    ":core:data-repository",
    ":core:database",
    ":core:designsystem",
    ":core:domain",
    ":core:navigation-runtime",
    ":core:navigation-contract",
    ":core:network",
    ":core:webview",
    ":feature:article:impl",
    ":feature:auth:impl",
    ":feature:collection:impl",
    ":feature:demo:impl",
    ":feature:home:impl",
    ":feature:search:impl",
    ":feature:user:impl",
    ":feature:picture:impl",
)

/** core 模块间允许的依赖矩阵（正向声明）。未声明的 core→core 边一律失败。 */
private val CORE_ALLOWED_DEPENDENCIES: Map<String, Set<String>> = mapOf(
    ":core:model" to emptySet(),
    ":core:android-platform" to emptySet(),
    ":core:navigation-contract" to emptySet(),
    ":core:navigation-runtime" to emptySet(),
    ":core:designsystem" to emptySet(),
    ":core:data-contract" to setOf(":core:model"),
    ":core:domain" to setOf(":core:model"),
    ":core:database" to setOf(":core:data-contract", ":core:model"),
    ":core:network" to setOf(":core:android-platform", ":core:data-contract", ":core:model"),
    ":core:data-repository" to setOf(
        ":core:domain",
        ":core:android-platform",
        ":core:data-contract",
        ":core:model",
    ),
    ":core:ui" to setOf(":core:designsystem"),
    ":core:player" to setOf(":core:android-platform"),
    ":core:webview" to setOf(":core:android-platform"),
)
