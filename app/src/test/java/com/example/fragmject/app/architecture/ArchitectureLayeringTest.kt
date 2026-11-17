package com.example.fragmject.app.architecture

import com.example.fragmject.app.architecture.ArchitectureSupport.APP_DATA_IMPORT_ALLOWLIST
import com.example.fragmject.app.architecture.ArchitectureSupport.hasFeatureImplImport
import com.example.fragmject.app.architecture.ArchitectureSupport.hasOtherFeatureApiImport
import com.example.fragmject.app.architecture.ArchitectureSupport.hasOtherFeatureImplImport
import com.example.fragmject.app.architecture.ArchitectureSupport.isFeatureApi
import com.example.fragmject.app.architecture.ArchitectureSupport.isFeatureImpl
import com.example.fragmject.app.architecture.ArchitectureSupport.productionFiles
import com.example.fragmject.app.architecture.ArchitectureSupport.readSource
import com.lemonappdev.konsist.api.verify.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 依赖方向与分层规则（feature → core 单向、core 不反向依赖、数据层边界）。
 *
 * 从 `ArchitectureTest` 拆出，规则内容与编号保持不变。
 */
class ArchitectureLayeringTest {

    /**
     * 规则 1：`core:domain` 不得依赖 `data / network / database`。
     * 领域层是纯契约层，只允许依赖 `core:model`。
     */
    @Test
    fun `core domain does not depend on data network database`() {
        productionFiles
            .filter { it.projectPath.contains("/core/domain/") }
            .assertFalse(
                additionalMessage = "core:domain 不得依赖 core:data-repository / core:network / core:database",
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
     * 规则 5：`core:data-repository`（数据实现层）不得依赖 `network / database`。
     * Repository / PagingSource 只依赖 `core:data-contract` 的 remote/local 端口，
     * 不得直接依赖 Retrofit Service、Room DAO/Entity 或 Store（阶段 4 验收标准第 6 条）。
     */
    @Test
    fun `core data does not depend on network database`() {
        productionFiles
            .filter { it.projectPath.contains("/core/data-repository/") }
            .assertFalse(
                additionalMessage = "core:data-repository 不得依赖 core:network / core:database（Repository/PagingSource 只依赖 data-contract 端口）",
            ) { file ->
                file.hasImport { imp ->
                    imp.name.startsWith("com.example.fragmject.core.network.") ||
                        imp.name.startsWith("com.example.fragmject.core.database.")
                }
            }
    }

    /**
     * 规则 6：`feature:*:api` 不得依赖 `feature:*:impl`。
     * api 是纯契约层（NavKey/接口），不得反向依赖实现层，保持依赖方向 impl → api 单向。
     */
    @Test
    fun `feature api does not depend on feature impl`() {
        productionFiles
            .filter { it.isFeatureApi() }
            .assertFalse(
                additionalMessage = "feature:*:api 不得依赖 feature:*:impl",
            ) { file -> file.hasFeatureImplImport() }
    }

    /**
     * 规则 7：`core` / `feature` 模块不得依赖 `:app`。
     * `:app` 是唯一组合根，依赖方向应为 app → feature/core，不可反向。
     */
    @Test
    fun `core and feature do not depend on app`() {
        productionFiles
            .filter { it.projectPath.contains("/core/") || it.projectPath.contains("/feature/") }
            .assertFalse(
                additionalMessage = "core / feature 模块不得依赖 :app",
            ) { file ->
                file.hasImport { imp -> imp.name.startsWith("com.example.fragmject.app.") }
            }
    }

    /**
     * 规则 12：feature 不得依赖数据层平台能力与数据实现。
     *
     * 所需平台能力必须经 core 引擎/端口获取：
     * - 缓存目录 / 清理 → [com.example.fragmject.core.domain.system.SystemStorage]
     * - 位图解码 / 编辑保存 → [com.example.fragmject.core.image.editor.ImageEditorEngine]
     *
     * feature 不得引用 data 层任何类型。
     */
    @Test
    fun `feature does not depend on data platform or data impl`() {
        productionFiles
            .filter { it.projectPath.contains("/feature/") }
            .assertFalse(
                additionalMessage = "feature 不得依赖 core:android-platform / data-repository / data-contract / network / database",
            ) { file ->
                file.hasImport { imp ->
                    when {
                        imp.name.startsWith("com.example.fragmject.core.android.platform.") -> true
                        imp.name.startsWith("com.example.fragmject.core.data.repository.") -> true
                        imp.name.startsWith("com.example.fragmject.core.data.contract.") -> true
                        imp.name.startsWith("com.example.fragmject.core.network.") -> true
                        imp.name.startsWith("com.example.fragmject.core.database.") -> true
                        else -> false
                    }
                }
            }
    }

    /**
     * 规则 16：`feature:*:api` 只允许引用纯契约（`core:navigation-contract`）。
     *
     * api 是「路由键契约」层（NavKey），不得引入领域端口（domain）、UI（ui/designsystem）、
     * 导航运行时机制（navigation-runtime）等运行时/领域模块，否则契约层被污染后失去可复用性。
     *
     * 本规则是 Gradle 守卫 R9（`FEATURE_API_ALLOWED_CORE_DEPENDENCIES`）的源码层对偶：
     * Gradle 只能校验「声明的 project 依赖边」，本规则按源码 import 校验，
     * 可捕获「靠传递路径编译到」的越界引用（Gradle 对这类引用无感知）。
     */
    @Test
    fun `feature api only depends on navigation contract`() {
        productionFiles
            .filter { it.isFeatureApi() }
            .assertFalse(
                additionalMessage = "feature:*:api 只允许引用 core:navigation-contract，不得 import domain/ui/designsystem/navigation-runtime 等机制模块",
            ) { file ->
                file.hasImport { imp ->
                    when {
                        imp.name.startsWith("com.example.fragmject.core.domain.") -> true
                        imp.name.startsWith("com.example.fragmject.core.ui.") -> true
                        imp.name.startsWith("com.example.fragmject.core.designsystem.") -> true
                        imp.name.startsWith("com.example.fragmject.core.navigation.runtime.") -> true
                        else -> false
                    }
                }
            }
    }

    /**
     * 规则 17：`core:database` 只允许实现白名单内的零逻辑链 domain 端口。
     *
     * History/Schedule/Theme 三条零逻辑链合并后，database 直接实现 domain 端口
     * （无「data-contract 本地端口 + data-repository 透传」两层中间层）。
     * OtpAccount 同属此类且**必须**落在 database：密钥须经 Keystore 加密
     * （core:android-platform），而 data-repository 受 R5 约束不得依赖 database，
     * 加密与 DAO 无法同处一层。
     *
     * 这些是**登记过的例外**（见 dependencies-policy.md §6.6），禁止继续扩散到其它
     * domain 端口——其它端口的实现必须落在 core:data-repository，否则守卫与文档会同时失守。
     */
    @Test
    fun `core database only implements whitelisted domain ports`() {
        val whitelist = setOf(
            "HistoryRepository",
            "ScheduleRepository",
            "ThemeRepository",
            "OtpAccountRepository",
        )
        productionFiles
            .filter { it.projectPath.contains("/core/database/") }
            .assertFalse(
                additionalMessage = "core:database 只允许实现 $whitelist（零逻辑链例外），其他 domain 端口的实现必须在 core:data-repository",
            ) { file ->
                file.hasImport { imp ->
                    imp.name.startsWith("com.example.fragmject.core.domain.repository.") &&
                        imp.name.substringAfterLast(".") !in whitelist
                }
            }
    }

    /**
     * 规则 21：`:app` 生产代码不得引用数据实现层（`core:network` / `core:database` /
     * `core:data-repository` / `core:data-contract`）。
     *
     * app 是组合根，负责 Hilt 聚合而非数据装配：直接引用数据层类型会让「换实现」变成改 app，
     * 也让 Gradle 白名单（允许 app 依赖 network/database）失去约束意义——Gradle 只能校验
     * 「声明的边」，校验不了源码 import，故由本规则补位。
     *
     * 存量例外（登记制，禁止新增）：[APP_DATA_IMPORT_ALLOWLIST]。
     */
    @Test
    fun `app does not import data implementation layer`() {
        val dataLayerImport = Regex(
            """import com\.example\.fragmject\.core\.(?:network|database|data\.repository|data\.contract)\.[\w.]+""",
        )
        val appFiles = productionFiles.filter { it.projectPath.contains("/app/") }
        assertTrue("app 选择器匹配 0 文件，检查 /app/ 路径", appFiles.isNotEmpty())
        val offenders = appFiles.flatMap { file ->
            val name = file.projectPath.substringAfterLast("/")
            val allowlist = APP_DATA_IMPORT_ALLOWLIST[name].orEmpty()
            dataLayerImport.findAll(readSource(file))
                .map { it.value.removePrefix("import ") }
                .filter { it !in allowlist }
                .map { "$name → $it" }
                .toList()
        }
        assertTrue(
            "以下 app 源码引用了数据实现层（装配应下沉到对应 core 模块；确需引用请登记进 APP_DATA_IMPORT_ALLOWLIST）：$offenders",
            offenders.isEmpty(),
        )
    }

    /**
     * 规则 22：`core:designsystem` 只允许是「设计令牌 + 主题」层，不得依赖其它业务/UI 模块。
     *
     * 2026-10 边界评审后，designsystem 只剩 `AppColors` / `AppSpacing` / `AppTheme` /
     * `LocalWindowSizeClass`；`TitleBar` / `BackIcon` / `BottomNavItem` / `NightSwitchButton`
     * 已迁入 `core:ui`。本规则锁定该边界，防止组件再被塞回设计令牌层（曾有一个 890 行的
     * 动画控件放在这里）。
     */
    @Test
    fun `core designsystem only depends on nothing but compose`() {
        val designSystemFiles = productionFiles.filter { it.projectPath.contains("/core/designsystem/") }
        assertTrue("core/designsystem 选择器匹配 0 文件", designSystemFiles.isNotEmpty())
        val offenders = designSystemFiles
            .filter { file ->
                file.hasImport { imp -> imp.name.startsWith("com.example.fragmject.") }
            }
            .map { it.projectPath }
        assertTrue(
            "core:designsystem 不得 import 任何项目内模块（$offenders）——它只放颜色/间距/主题",
            offenders.isEmpty(),
        )
    }

    /**
     * 规则 23：`core:ui` 不得下探数据实现层。
     *
     * `core:ui` 依赖 `core:model` / `core:domain` 是登记过的例外（见 Gradle 守卫
     * `CORE_ALLOWED_DEPENDENCIES`），但**仅此而已**：一旦开始 import 数据实现层，
     * 通用 UI 组件就会绑死具体数据来源，且会绕过 R1/R5 的分层约束。
     */
    @Test
    fun `core ui does not depend on data implementation`() {
        productionFiles
            .filter { it.projectPath.contains("/core/ui/") }
            .assertFalse(
                additionalMessage = "core:ui 不得依赖 core:data-repository / network / database（只允许 designsystem / model / domain）",
            ) { file ->
                file.hasImport { imp ->
                    imp.name.startsWith("com.example.fragmject.core.data.") ||
                        imp.name.startsWith("com.example.fragmject.core.network.") ||
                        imp.name.startsWith("com.example.fragmject.core.database.")
                }
            }
    }
}
