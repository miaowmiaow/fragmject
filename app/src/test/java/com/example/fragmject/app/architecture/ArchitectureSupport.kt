package com.example.fragmject.app.architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.declaration.KoFileDeclaration

/**
 * 架构测试的共享扫描结果与辅助判定（由各主题测试类复用）。
 *
 * 拆分说明：原 `ArchitectureTest` 把 33 条规则放在一个类里（948 行）。规则本身不变，
 * 只按主题分到几个文件；共享的扫描与判定集中在这里，避免各文件重复定义。
 * 规则编号（如「规则 21」）保持不变，文档中的引用继续有效。
 */
internal object ArchitectureSupport {

    /** 只扫描 main sourceSet 的生产代码，排除 build 生成物与测试代码。 */
    val productionFiles: List<KoFileDeclaration> by lazy {
        Konsist.scopeFromProject().files.filter { it.isMainSource() }
    }

    /** 单文件行数上限，见规则 28。 */
    const val MAX_FILE_LINES = 500

    /** 接近上限的预警阈值（只输出告警，不失败）。 */
    const val WARN_FILE_LINES = 400

    /** 规则 21 allowlist 的条目数上限，见规则 21 的 allowlist 上限测试。 */
    const val APP_DATA_ALLOWLIST_CAP = 1

    /** 规则 21 的存量例外白名单：文件名 → 允许的数据层 import（禁止新增条目）。 */
    val APP_DATA_IMPORT_ALLOWLIST: Map<String, Set<String>> = mapOf(
        "FragmjectApplication.kt" to setOf("com.example.fragmject.core.network.di.ImageOkHttpClient"),
    )

    /**
     * 规则 31b allowlist 的条目数上限：只允许收缩，新增必须走 §6.6 登记。
     *
     * 没有上限时这个集合会变成「自我豁免名单」——往里加一行就能绕过登记流程，
     * 规则 31b 等于可以自己给自己开口子。与 [APP_DATA_ALLOWLIST_CAP] 同一思路。
     */
    const val APP_CORE_AGGREGATE_CAP = 7

    /**
     * 规则 31b：`APP_ALLOWED_DEPENDENCIES` 中属于「数据 / 运行时聚合」的 core 边。
     *
     * 这些边是 Hilt 聚合与导航装配的必需品（app 源码往往零 import），不需要逐条写理由，
     * 因此不必进 §6.6。判据是「**有没有直接使用能力**」，不是「是不是 core 模块」：
     * - 在名单内：`:app` 只是为了装配（`@Module` / 导航渲染器 / 主题包装），源码里不调用具体能力；
     * - 不在名单内（如 `:core:pet`、`:core:webview`）：`:app` 直接调用了它的能力
     *   （`webViewPool.prepare()` / `setDebuggingEnabled()`、`FishPet`），必须在 §6.6 登记并写理由。
     *
     * 条目数受 [APP_CORE_AGGREGATE_CAP] 约束（只允许减少）。
     */
    val APP_CORE_AGGREGATE_MODULES: Set<String> = setOf(
        // Hilt 聚合：@Module/@InstallIn 需要进入 SingletonComponent
        ":core:data-repository",
        ":core:database",
        ":core:network",
        // 应用级装配：主题/窗口尺寸/语义导航
        ":core:designsystem",
        ":core:domain",
        ":core:navigation-contract",
        ":core:navigation-runtime",
    )

    /** 守卫判定规则源码路径（规则 31 读取其中的 CORE_EXCEPTION_EDGES）。 */
    const val GUARD_RULES_PATH =
        "../build-logic/convention/src/main/kotlin/com/example/fragmject/convention/GuardRules.kt"

    /** 通过项目根目录读取源码文本，兼容 Gradle 以模块目录为工作目录的情况。 */
    fun readSource(file: KoFileDeclaration): String {
        val root = generateSequence(java.io.File(System.getProperty("user.dir") ?: ".")) { it.parentFile }
            .first { java.io.File(it, "settings.gradle.kts").isFile }
        return java.io.File(root, file.projectPath.removePrefix("/")).readText()
    }

    /** 判断文件是否属于 main 生产源集（路径包含 `/src/main/`）。 */
    fun KoFileDeclaration.isMainSource(): Boolean = projectPath.contains("/src/main/")

    fun KoFileDeclaration.isFeatureImpl(): Boolean =
        projectPath.contains("/feature/") && projectPath.contains("/impl/")

    /**
     * UI 层选择器：`core:ui`（通用组件）与 `core:pet`（漂浮宠物，2026-11 拆出的独立模块）。
     *
     * 两条约束按同一口径作用于两者——「UI 层不得下探数据层」「UI 层不得混入数据/媒体提供类 API」。
     * 抽成共用判定而不是各写一份 `contains("/core/ui/")`，是为了以后再加 UI 层模块时不再漏一处
     * （宠物拆模块时正是因此差点让规则 11 / 23 对宠物静默失效）。
     *
     * 允许依赖不同：`core:ui` 另有 model / domain 两条登记例外，`core:pet` 只允许 designsystem。
     */
    fun KoFileDeclaration.isUiLayerFile(): Boolean =
        projectPath.contains("/core/ui/") || projectPath.contains("/core/pet/")

    fun KoFileDeclaration.isFeatureApi(): Boolean =
        projectPath.contains("/feature/") && projectPath.contains("/api/")

    fun KoFileDeclaration.featureName(): String? {
        val marker = "/feature/"
        val idx = projectPath.indexOf(marker)
        if (idx < 0) return null
        return projectPath.substring(idx + marker.length).substringBefore("/")
    }

    /**
     * 判定文件是否 import 了其他 feature 的 api 类（6 段包名，如 NavKey）。
     * 包名 `com.example.fragmject.feature.<name>.<Class>` 共 6 段，其中 segments[4] 为 feature 名。
     */
    fun KoFileDeclaration.hasOtherFeatureApiImport(): Boolean {
        val own = featureName() ?: return false
        val prefix = "com.example.fragmject.feature."
        return hasImport { imp ->
            if (!imp.name.startsWith(prefix)) return@hasImport false
            val segments = imp.name.split(".")
            segments.size == 6 && segments[4] != own
        }
    }

    /**
     * 判定文件是否 import 了其他 feature 的 impl 类（7+ 段包名）。
     * api 模块类无子包（6 段），impl 模块类带子包（如 `..feature.<name>.nav.Xxx`，7+ 段）。
     */
    fun KoFileDeclaration.hasOtherFeatureImplImport(): Boolean {
        val own = featureName() ?: return false
        val prefix = "com.example.fragmject.feature."
        return hasImport { imp ->
            if (!imp.name.startsWith(prefix)) return@hasImport false
            val segments = imp.name.split(".")
            segments.size > 6 && segments[4] != own
        }
    }

    /**
     * 判定文件是否 import 了任何 feature impl 类（7+ 段包名）。
     * 用于「feature:*:api 不得依赖 feature:*:impl」规则：api 模块自身不含 impl 子包，
     * 任何 >6 段的 feature 导入都是对 impl 的反向依赖，因此不区分 own。
     */
    fun KoFileDeclaration.hasFeatureImplImport(): Boolean {
        val prefix = "com.example.fragmject.feature."
        return hasImport { imp ->
            imp.name.startsWith(prefix) && imp.name.split(".").size > 6
        }
    }
}
