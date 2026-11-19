package com.example.fragmject.convention

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ExternalModuleDependency
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
 *
 * ## 与 Configuration Cache 的关系
 * 依赖图信息（边、各模块声明的 `androidx.core` 构件、模块目录映射）全部在**配置阶段**
 * 采集，写成任务的不可变输入；任务执行阶段只读输入与源码文件，不再访问 `Project`。
 * 因此本任务可以参与配置缓存——旧实现在 `doLast` 里遍历 `root.subprojects` 与
 * `configurations`，只能声明 `notCompatibleWithConfigurationCache`，只要与其它任务
 * 同批执行就会让整个配置缓存条目被丢弃。
 *
 * 采集点是 [org.gradle.api.invocation.Gradle.projectsEvaluated]：根插件 apply 时
 * 子项目尚未配置，只有全部项目配置完成后才能读到各模块最终声明的依赖。
 */
class FragmjectAndroidDependencyGuardPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        if (target != target.rootProject) return

        val guard = target.tasks.register(
            "verifyModuleDependencies",
            VerifyModuleDependenciesTask::class.java,
        ) {
            group = "verification"
            description =
                "校验模块间 Gradle 依赖方向，并禁止源码使用外部库却依赖传递路径（隐式依赖）"
            // 源码范围在配置阶段固定；文件内容由 Gradle 快照（相对路径参与指纹，可重定位），
            // 执行阶段只读取快照里的文件，因此不需要（也不应该）在执行期扫描 Project。
            sourceFiles.from(
                target.fileTree(target.rootDir) {
                    include("**/src/main/**/*.kt")
                    // build-logic 是独立构建，它的 src/main 不属于任何子项目
                    exclude("**/build/**", "build-logic/**")
                },
            )
        }

        target.gradle.projectsEvaluated {
            val model = collectGuardModel(target)
            guard.configure {
                moduleEdges.set(model.edges)
                declaredCoreArtifacts.set(model.declaredCoreArtifacts)
                moduleSourceRoots.set(model.sourceRoots)
            }
        }
    }
}

/** 配置阶段采集出的、与 Gradle 模型无关的守卫输入（纯字符串，可安全进入配置缓存）。 */
private class GuardModel(
    /** 模块路径 -> 直接依赖的模块路径。 */
    val edges: Map<String, List<String>>,
    /** 模块路径 -> 本模块显式声明的 `androidx.core` 构件名；仅含参与隐式依赖校验的模块。 */
    val declaredCoreArtifacts: Map<String, List<String>>,
    /** 模块目录（相对根目录，`/` 分隔） -> 模块路径。 */
    val sourceRoots: Map<String, String>,
)

/** 采集全部子项目的依赖图信息。只在配置阶段调用（`projectsEvaluated`）。 */
private fun collectGuardModel(root: Project): GuardModel {
    val rootDir = root.projectDir
    val edges = mutableMapOf<String, List<String>>()
    val declared = mutableMapOf<String, List<String>>()
    val sourceRoots = mutableMapOf<String, String>()

    for (sub in root.subprojects) {
        // 目录映射对所有模块都要有：任务按目录把源码文件归属到模块，
        // 缺映射会让文件无法归属而失败（见 VerifyModuleDependenciesTask）。
        sourceRoots[sub.projectDir.relativeTo(rootDir).invariantSeparatorsPath] = sub.path
        // 测试专用模块不进生产依赖图，也不参与隐式依赖校验
        if (sub.path in TEST_ONLY_MODULES) continue
        edges[sub.path] = declaredProjectDependencies(sub)
        declared[sub.path] = declaredCoreArtifacts(sub)
    }
    return GuardModel(edges, declared, sourceRoots)
}

/** 模块的直接依赖边（排除测试配置）；模块路径已排序，保证任务输入稳定。 */
private fun declaredProjectDependencies(sub: Project): List<String> {
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
    return projectDeps.sorted()
}

/** 本模块显式声明的 `androidx.core` 构件名集合（排除测试配置）。 */
private fun declaredCoreArtifacts(sub: Project): List<String> {
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
    return names.sorted()
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
