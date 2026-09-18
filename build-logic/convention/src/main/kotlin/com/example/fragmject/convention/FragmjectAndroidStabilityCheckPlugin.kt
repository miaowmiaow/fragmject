package com.example.fragmject.convention

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Compose 稳定性配置校验插件（id = "fragmject.android.stability-check"）。
 *
 * 在根项目注册 `verifyComposeStabilityConfig` 任务：
 * 校验 compose_stability_config.conf 中列出的每个类在源码中真实存在，
 * 防止类被删除/重命名后配置残留为「幽灵类」，导致 Compose 编译器静默忽略。
 *
 * 仅校验存在性（幽灵类检测）；「可变类被误标记稳定」这类语义问题仍靠人工 review。
 */
class FragmjectAndroidStabilityCheckPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        if (target != target.rootProject) return
        val rootDir = target.rootProject.projectDir

        target.tasks.register("verifyComposeStabilityConfig") {
            group = "verification"
            description = "校验 compose_stability_config.conf 无幽灵类"

            doLast {
                val configFile = rootDir.resolve("compose_stability_config.conf")
                if (!configFile.exists()) {
                    throw GradleException("未找到稳定性配置文件：${configFile.name}")
                }

                val classNames = configFile.readLines()
                    .map { it.trim() }
                    .filter { it.isNotEmpty() && !it.startsWith("#") }

                // 读取所有 src/main/java 下的 .kt 文件内容，按类型声明匹配
                // （Kotlin 一个文件可含多个顶层类，不能按「类名 = 文件名」匹配）
                val sourceContents = rootDir.walkTopDown()
                    .filter { it.isFile }
                    .filter {
                        it.extension == "kt" && it.path.replace('\\', '/')
                            .contains("/src/main/java/")
                    }.joinToString("\n") { it.readText() }

                val missing = classNames.filter { className ->
                    val simpleName = className.substringAfterLast('.')
                    val declared = listOf(
                        "class $simpleName",
                        "interface $simpleName",
                        "object $simpleName",
                    ).any { sourceContents.contains(it) }
                    !declared
                }

                if (missing.isNotEmpty()) {
                    throw GradleException(
                        "compose_stability_config.conf 含幽灵类（源码中无定义）：\n" +
                            missing.joinToString("\n") { "  - $it" }
                    )
                }
            }
        }
    }
}
