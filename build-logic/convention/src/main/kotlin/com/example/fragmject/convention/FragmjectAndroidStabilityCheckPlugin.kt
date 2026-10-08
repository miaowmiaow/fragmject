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
 *
 * ## compose_stability_config.conf 维护规则
 * 该文件由 Compose 编译器直接解析，格式为「每行一个类名 pattern」，不支持 # 注释，
 * 因此维护约定集中记录于此（务必遵守）：
 * 1. 仅收录「所有字段均为 val 且字段类型不可变」的数据类。
 * 2. 含 var 可变字段或 MutableList/MutableSet 等可变集合的类【不要】收录，
 *    否则会导致重组被跳过、UI 不更新。已排除示例：
 *    - Coin（open class + var username）
 *    - MyCoin（继承 Coin，间接持有 var username）
 *    - Tree（var childrenSelectPosition）
 *    - CoinRankData（持有 List<Coin>，Coin 含 var username）
 *    - MyCoinData（持有 List<MyCoin>，MyCoin 继承 Coin）
 * 3. 新增 core/model 数据类后，必须同步追加到配置文件，否则会被视为不稳定、造成静默性能退化。
 * 4. 删除/重命名数据类后，必须同步移除配置对应行，否则产生幽灵类（本任务会检测）。
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

                // 扫描范围限定 core/model/src/main，避免误扫 build/ 产物
                val sourceDir = rootDir.resolve("core/model/src/main")
                val sourceContents = if (sourceDir.exists()) {
                    sourceDir.walkTopDown()
                        .filter { it.isFile && it.extension == "kt" }
                        .joinToString("\n") { it.readText() }
                } else {
                    ""
                }

                val missing = classNames.filter { className ->
                    val simpleName = className.substringAfterLast('.')
                    // 词边界 \b 避免前缀误匹配（如 Article 命中 ArticleData）
                    val declared = Regex("(class|interface|object)\\s+${Regex.escape(simpleName)}\\b")
                        .containsMatchIn(sourceContents)
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
