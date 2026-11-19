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
