package com.example.fragmject.app.architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.declaration.KoFileDeclaration
import com.lemonappdev.konsist.api.verify.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 架构一致性测试（阶段 0 建立，D2 决策：fail 模式）。
 *
 * 规则以源码 import 为依据，通过 Konsist 解析整个项目的 Kotlin 文件。
 * 运行入口：`./gradlew :app:testFreeDebugUnitTest --tests "*ArchitectureTest"`
 *
 * 路径约定（Konsist 的 [KoFileDeclaration.projectPath] 为相对项目根、以 `/` 开头的路径）：
 * - feature api 模块类：`com.example.fragmject.feature.<name>.<Class>`（6 段包名，如 NavKey）
 * - feature impl 模块类：`com.example.fragmject.feature.<name>.<sub>.<Class>`（7+ 段包名）
 * - core 模块类：`com.example.fragmject.core.<module>.<...>`
 */
class ArchitectureTest {

    /** 只扫描 main sourceSet 的生产代码，排除 build 生成物与测试代码。 */
    private val productionFiles: List<KoFileDeclaration> = Konsist.scopeFromProject()
        .files
        .filter { it.isMainSource() }

    /**
     * 规则 1：`core:domain` 不得依赖 `data / network / database`。
     * 领域层是纯契约层，只允许依赖 `core:model`。
     */
    @Test
    fun `core domain does not depend on data network database`() {
        productionFiles
            .filter { it.projectPath.contains("/core/domain/") }
            .assertFalse(
                additionalMessage = "core:domain 不得依赖 core:data-impl / core:network / core:database",
            ) { file ->
                file.hasImport { imp ->
                    imp.name.startsWith("com.example.fragmject.core.data.") ||
                        imp.name.startsWith("com.example.fragmject.core.network.") ||
                        imp.name.startsWith("com.example.fragmject.core.database.")
                }
            }
    }

    /**
     * 规则 1b：`core:domain` 的分页依赖仅限纯 JVM 的 paging-common（PagingData 载体），
     * 禁止引入含 Android framework 的 paging-runtime（Pager/PagingSource 等实现细节）。
     */
    @Test
    fun `core domain only uses paging common not paging runtime`() {
        productionFiles
            .filter { it.projectPath.contains("/core/domain/") }
            .assertFalse(
                additionalMessage = "core:domain 分页依赖仅限 paging-common，禁止 paging-runtime（含 Android framework）",
            ) { file ->
                file.hasImport { imp ->
                    imp.name.startsWith("androidx.paging.runtime.")
                }
            }
    }

    /**
     * 规则 2：`feature:*:impl` 不得依赖其他 `feature:*:impl`。
     * 当前已满足（锁定），任何新增跨 impl 依赖立即 fail。
     */
    @Test
    fun `feature impl does not depend on other feature impl`() {
        productionFiles
            .filter { it.isFeatureImpl() }
            .assertFalse(
                additionalMessage = "feature:*:impl 不得依赖其他 feature:*:impl",
            ) { file -> file.hasOtherFeatureImplImport() }
    }

    /**
     * 规则 3：`feature:*:impl` 不得依赖其他 `feature:*:api`。
     * 阶段 1 已通过语义 Navigator 消除全部存量跨域依赖，本规则直接锁定。
     */
    @Test
    fun `feature impl does not depend on other feature api`() {
        productionFiles
            .filter { it.isFeatureImpl() }
            .assertFalse(
                additionalMessage = "feature:*:impl 不得依赖其他 feature:*:api",
            ) { file -> file.hasOtherFeatureApiImport() }
    }

    /**
     * 规则 4：`core` 模块不得反向依赖任何 `feature` 模块。
     * 保证 `:app` 是唯一能看到全部 feature api 的组合根，依赖方向单向（feature → core）。
     */
    @Test
    fun `core does not depend on feature`() {
        productionFiles
            .filter { it.projectPath.contains("/core/") }
            .assertFalse(
                additionalMessage = "core 模块不得反向依赖 feature 模块（依赖方向应为 feature → core）",
            ) { file ->
                file.hasImport { imp -> imp.name.startsWith("com.example.fragmject.feature.") }
            }
    }

    /**
     * 规则 5：`core:data-impl`（数据实现层）不得依赖 `network / database`。
     * Repository / PagingSource 只依赖 `core:data-contract` 的 remote/local 端口，
     * 不得直接依赖 Retrofit Service、Room DAO/Entity 或 Store（阶段 4 验收标准第 6 条）。
     */
    @Test
    fun `core data does not depend on network database`() {
        productionFiles
            .filter { it.projectPath.contains("/core/data-impl/") }
            .assertFalse(
                additionalMessage = "core:data-impl 不得依赖 core:network / core:database（Repository/PagingSource 只依赖 data-contract 端口）",
            ) { file ->
                file.hasImport { imp ->
                    imp.name.startsWith("com.example.fragmject.core.network.") ||
                        imp.name.startsWith("com.example.fragmject.core.database.")
                }
            }
    }

    /**
     * 守卫测试：确保各规则的选择器能匹配到真实文件，避免选择器写错导致规则空转（永远通过）。
     */
    @Test
    fun `boundary selectors match real files`() {
        assertTrue(
            "core/domain 选择器匹配 0 文件，检查 projectPath 前缀",
            productionFiles.any { it.projectPath.contains("/core/domain/") },
        )
        assertTrue(
            "feature impl 选择器匹配 0 文件，检查 projectPath 前缀",
            productionFiles.any { it.isFeatureImpl() },
        )
        assertTrue(
            "core/data-impl 选择器匹配 0 文件，检查 projectPath 前缀",
            productionFiles.any { it.projectPath.contains("/core/data-impl/") },
        )
    }

    // ---- 辅助判定 ----

    /** 判断文件是否属于 main 生产源集（路径包含 `/src/main/`）。 */
    private fun KoFileDeclaration.isMainSource(): Boolean =
        projectPath.contains("/src/main/")

    private fun KoFileDeclaration.isFeatureImpl(): Boolean =
        projectPath.contains("/feature/") && projectPath.contains("/impl/")

    private fun KoFileDeclaration.featureName(): String? {
        val marker = "/feature/"
        val idx = projectPath.indexOf(marker)
        if (idx < 0) return null
        return projectPath.substring(idx + marker.length).substringBefore("/")
    }

    /**
     * 判定文件是否 import 了其他 feature 的 api 类（6 段包名，如 NavKey）。
     * 包名 `com.example.fragmject.feature.<name>.<Class>` 共 6 段，其中 segments[4] 为 feature 名。
     */
    private fun KoFileDeclaration.hasOtherFeatureApiImport(): Boolean {
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
    private fun KoFileDeclaration.hasOtherFeatureImplImport(): Boolean {
        val own = featureName() ?: return false
        val prefix = "com.example.fragmject.feature."
        return hasImport { imp ->
            if (!imp.name.startsWith(prefix)) return@hasImport false
            val segments = imp.name.split(".")
            segments.size > 6 && segments[4] != own
        }
    }

}
