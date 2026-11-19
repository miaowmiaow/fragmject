package com.example.fragmject.convention

import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileVisitDetails
import org.gradle.api.provider.MapProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

/**
 * `verifyModuleDependencies` 的执行体（模块依赖方向 + 隐式外部依赖校验）。
 *
 * ## 为什么单独拆成一个 task 类
 * 与 Configuration Cache 兼容的前提是「执行阶段不访问 Project / Configuration」。
 * 因此需要 Gradle 模型的信息全部在配置阶段采集为输入（采集逻辑见
 * [FragmjectAndroidDependencyGuardPlugin]），本类的 [TaskAction] 只读输入与输入文件。
 * 旧实现把所有逻辑写在 `doLast` 闭包里、执行期遍历 `root.subprojects` 与
 * `configurations`，只能声明 `notCompatibleWithConfigurationCache`。
 *
 * ## 输入
 * - [moduleEdges] / [declaredCoreArtifacts] / [moduleSourceRoots]：纯字符串数据；
 * - [sourceFiles]：全部模块 `src/main` 下的 Kotlin 源码，按相对路径参与指纹（可重定位）。
 */
abstract class VerifyModuleDependenciesTask : DefaultTask() {

    /** 模块路径 -> 直接依赖的模块路径。 */
    @get:Input
    abstract val moduleEdges: MapProperty<String, List<String>>

    /** 模块路径 -> 本模块显式声明的 `androidx.core` 构件名；缺失的模块不参与隐式依赖校验。 */
    @get:Input
    abstract val declaredCoreArtifacts: MapProperty<String, List<String>>

    /** 模块目录（相对根目录，`/` 分隔） -> 模块路径，用于把源码文件归属到模块。 */
    @get:Input
    abstract val moduleSourceRoots: MapProperty<String, String>

    /** 全部模块 `src/main` 下的 Kotlin 源码。 */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sourceFiles: ConfigurableFileCollection

    @TaskAction
    fun verify() {
        val edges = moduleEdges.get().mapValues { it.value.toSet() }
        // 采集失效时必须失败：空图会让守卫「无声通过」，比报错更危险。
        if (edges.isEmpty()) {
            throw GradleException("依赖图采集为空：配置阶段未收集到任何模块依赖边")
        }

        val violations = mutableListOf<String>()
        violations.addAll(collectGraphViolations(edges))
        violations.addAll(collectImplicitExternalDeps())
        if (violations.isNotEmpty()) {
            throw GradleException(
                "模块依赖违规：\n" + violations.joinToString("\n") { "  - $it" }
            )
        }
    }

    /** 依赖图（声明的边）层面的违规：环、app 聚合、单条边方向。 */
    private fun collectGraphViolations(edges: Map<String, Set<String>>): List<String> {
        val violations = mutableListOf<String>()

        // 环检测：模块依赖图必须是无环 DAG，任何依赖环都属架构违规
        detectCycles(edges).forEach { cycle ->
            violations.add("检测到依赖环：$cycle")
        }

        // 所有 feature:*:impl 必须被 app 组合根聚合，
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
    private fun collectImplicitExternalDeps(): List<String> {
        val declaredByModule = declaredCoreArtifacts.get()
        // 最长前缀优先：模块目录可能互相嵌套（如 core 与 core:ui）
        val dirPrefixes = moduleSourceRoots.get().entries
            .map { (dir, module) -> "$dir/" to module }
            .sortedByDescending { it.first.length }

        val usedPackages = mutableMapOf<String, MutableSet<String>>()
        val unattributed = mutableListOf<String>()

        // 必须用显式 Action 对象：FileTree.visit 另有 Closure 重载，lambda 会被解析到 Closure 上
        sourceFiles.asFileTree.visit(
            object : Action<FileVisitDetails> {
                override fun execute(details: FileVisitDetails) {
                    if (details.isDirectory) return
                    val relativePath = details.relativePath.pathString
                    val module = dirPrefixes
                        .firstOrNull { relativePath.startsWith(it.first) }
                        ?.second
                    if (module == null) {
                        unattributed.add(relativePath)
                        return
                    }
                    val packages = usedPackages.getOrPut(module) { mutableSetOf() }
                    details.file.useLines { lines ->
                        lines.forEach { line ->
                            parseCoreImportPackage(line)?.let(packages::add)
                        }
                    }
                }
            },
        )

        // 源码树里的文件必须能归属到某个模块：归属失败说明目录映射失效，
        // 静默跳过会让这些模块的隐式依赖检查凭空消失。
        if (unattributed.isNotEmpty()) {
            val sample = unattributed.take(5).joinToString("\n") { "  - $it" }
            val more = if (unattributed.size > 5) "\n  ...（共 ${unattributed.size} 个）" else ""
            throw GradleException("无法把源码文件归属到模块（模块目录映射失效）：\n$sample$more")
        }

        val violations = mutableListOf<String>()
        for ((module, packages) in usedPackages) {
            // 未采集到声明的模块（如测试专用模块 :benchmark）不参与本校验
            val declared = declaredByModule[module]?.toSet() ?: continue
            unmetCoreImports(packages, declared).forEach { (pkg, required) ->
                violations.add(
                    "$module 源码使用 androidx.core.$pkg.*，但未显式声明 $required" +
                        "（禁止依赖传递路径：上游调整传递关系即编译失败）"
                )
            }
        }
        // 排序保证输出稳定（文件遍历顺序不作为契约）
        return violations.sorted()
    }
}
