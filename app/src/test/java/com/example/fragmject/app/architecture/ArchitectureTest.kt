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
 * 运行入口：`./gradlew :app:testDebugUnitTest --tests "*ArchitectureTest"`
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
     * 规则 8：`core:model` / `core:domain` / `core:data-contract` 不得依赖 Android framework。
     * 纯契约 / 数据层必须保持平台无关，禁止任何 `android.*`（framework）导入。
     * 注：`core:designsystem` 为 Compose UI 层，`android.view.Window` 等属合法 UI 能力，不在此列。
     */
    @Test
    fun `core contract layers do not depend on android framework`() {
        productionFiles
            .filter {
                it.projectPath.contains("/core/model/") ||
                    it.projectPath.contains("/core/domain/") ||
                    it.projectPath.contains("/core/data-contract/")
            }
            .assertFalse(
                additionalMessage = "core:model / core:domain / core:data-contract 不得依赖 android.*（framework）",
            ) { file ->
                file.hasImport { imp -> imp.name.startsWith("android.") }
            }
    }

    /**
     * 规则 9：`core:data-repository` 仅允许白名单内的 Android framework 导入。
     * 媒体库（MediaStore / ContentUris）已下沉 core:android-platform，此处只允许
     * Context / Environment / Log 基础设施，其余 `android.*` 一律失败。
     */
    @Test
    fun `core data repository only allows whitelisted android imports`() {
        productionFiles
            .filter { it.projectPath.contains("/core/data-repository/") }
            .assertFalse(
                additionalMessage = "core:data-repository 仅允许 Context/Environment/Log",
            ) { file ->
                file.hasImport { imp ->
                    imp.name.startsWith("android.") &&
                        imp.name !in setOf(
                            "android.content.Context",
                            "android.os.Environment",
                            "android.util.Log",
                        )
                }
            }
    }

    /**
     * 规则 10：`core:designsystem` 仅禁止数据 / 媒体提供类 API 混入。
     * UI 层允许 android.view.Window、android.annotation.SuppressLint、android.graphics 等合法 UI 能力，
     * 但禁止 android.provider.* / android.content.ContentResolver / android.media.* / android.database.*，
     * 防止媒体查询或数据库代码误放进设计系统。
     */
    @Test
    fun `core designsystem does not depend on data provider apis`() {
        productionFiles
            .filter { it.projectPath.contains("/core/designsystem/") }
            .assertFalse(
                additionalMessage = "core:designsystem 不得依赖 android.provider.* / android.content.ContentResolver / android.media.* / android.database.*",
            ) { file ->
                file.hasImport { imp ->
                    imp.name.startsWith("android.provider.") ||
                        imp.name == "android.content.ContentResolver" ||
                        imp.name.startsWith("android.media.") ||
                        imp.name.startsWith("android.database.")
                }
            }
    }

    /**
     * 规则 11：`core:ui` 仅禁止数据 / 媒体提供类 API 混入。
     * UI 组件库允许 BitmapFactory / android.text.Html / displayMetrics 等合法 UI 能力，
     * 但禁止 android.provider.* / android.content.ContentResolver / android.media.* / android.database.*，
     * 防止媒体查询或数据库代码误放进 UI 组件库。
     */
    @Test
    fun `core ui does not depend on data provider apis`() {
        productionFiles
            .filter { it.projectPath.contains("/core/ui/") }
            .assertFalse(
                additionalMessage = "core:ui 不得依赖 android.provider.* / android.content.ContentResolver / android.media.* / android.database.*",
            ) { file ->
                file.hasImport { imp ->
                    imp.name.startsWith("android.provider.") ||
                        imp.name == "android.content.ContentResolver" ||
                        imp.name.startsWith("android.media.") ||
                        imp.name.startsWith("android.database.")
                }
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
     * 规则 13：`android.webkit.*` 只允许出现在 `core:webview`。
     *
     * WebView 平台类型（WebView / CookieManager / WebResourceRequest…）是 UI 基础设施的
     * 实现细节；其他模块必须经 core:webview 的封装使用：
     * - 链接判定 / 长按命中 → [WebViewCommons]
     * - 调试开关 → WebViewPool.setDebuggingEnabled
     *
     * 例外：`feature:demo:impl` 是平台 API 演示模块，允许直接使用 WebView 演示原生能力，
     * 业务 feature 不得效仿。
     */
    @Test
    fun `android webkit is confined to core webview`() {
        productionFiles
            .filterNot {
                it.projectPath.contains("/core/webview/") ||
                    it.projectPath.contains("/feature/demo/impl/")
            }
            .assertFalse(
                additionalMessage = "android.webkit.* 只允许出现在 core:webview（feature:demo:impl 为演示模块例外）",
            ) { file -> file.hasImport { imp -> imp.name.startsWith("android.webkit.") } }
    }

    /**
     * 规则 14：feature 内创建 ViewModel 必须使用 `hiltViewModel()`。
     *
     * 项目已接入 entry 级 ViewModelStore（[rememberViewModelStoreNavEntryDecorator]），
     * 单栏页面的 ViewModel 随 NavEntry 出栈清除；entry owner 不带 Hilt 工厂，
     * 使用 lifecycle 的 `viewModel()` 会导致注入失败或退化成 Activity 作用域。
     *
     * 例外：`app` 组合根在 NavDisplay 之外创建的 ViewModel（如 AppNavViewModel）
     * 本就应是 Activity 作用域，不在本规则约束范围内。
     */
    @Test
    fun `feature creates viewmodel with hiltViewModel`() {
        productionFiles
            .filter { it.projectPath.contains("/feature/") }
            .assertFalse(
                additionalMessage = "feature 必须使用 hiltViewModel()，禁止 androidx.lifecycle.viewmodel.compose.viewModel",
            ) { file ->
                file.hasImport { imp ->
                    imp.name == "androidx.lifecycle.viewmodel.compose.viewModel"
                }
            }
    }

    /**
     * 规则 15：`CookieStore` 的实现不得出现在 core 内部除 webview 之外的模块。
     *
     * [com.example.fragmject.core.domain.session.CookieStore] 的唯一实现是
     * core:webview 的 `WebViewCookieStore`（android.webkit.CookieManager），
     * 由 core:webview/di/CookieStoreModule 经 @Binds 绑定，再由 :app 组合根聚合注入。
     *
     * core:network 只允许构造注入该端口（CookieJar），不得自行实现：network 不允许
     * 依赖 webview（Gradle 守卫 CORE_ALLOWED_DEPENDENCIES），就地实现会绕过该约束，
     * 并造成 WebView 与 OkHttp 两个会话真相源。
     *
     * 只扫描 main 源集：测试里的 Fake 实现（CookieJarTest）不受约束。
     */
    @Test
    fun `cookie store implementation only in core webview`() {
        productionFiles
            .filter { it.projectPath.contains("/core/") }
            .filterNot { it.projectPath.contains("/core/webview/") }
            .assertFalse(
                additionalMessage = "CookieStore 实现只允许出现在 core:webview（唯一实现 WebViewCookieStore），其他 core 模块只允许注入使用",
            ) { file ->
                file.classes().any { klass ->
                    klass.countParents { parent -> parent.name == "CookieStore" } > 0
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
     * 规则 17：`core:database` 只允许实现三个零逻辑链 domain 端口。
     *
     * History/Schedule/Theme 三条零逻辑链合并后，database 直接实现 domain 端口
     * （无「data-contract 本地端口 + data-repository 透传」两层中间层）。
     * 这是**登记过的例外**（见 dependencies-policy.md §6.6），禁止扩散到其它 domain 端口——
     * 其它端口的实现必须落在 core:data-repository，否则守卫与文档会同时失守。
     */
    @Test
    fun `core database only implements whitelisted domain ports`() {
        val whitelist = setOf("HistoryRepository", "ScheduleRepository", "ThemeRepository")
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
            "feature api 选择器匹配 0 文件，检查 projectPath 前缀",
            productionFiles.any { it.isFeatureApi() },
        )
        assertTrue(
            "core/data-repository 选择器匹配 0 文件，检查 projectPath 前缀",
            productionFiles.any { it.projectPath.contains("/core/data-repository/") },
        )
        assertTrue(
            "core/designsystem 选择器匹配 0 文件，检查 projectPath 前缀",
            productionFiles.any { it.projectPath.contains("/core/designsystem/") },
        )
        assertTrue(
            "core/ui 选择器匹配 0 文件，检查 projectPath 前缀",
            productionFiles.any { it.projectPath.contains("/core/ui/") },
        )
        assertTrue(
            "core/model 选择器匹配 0 文件，检查 projectPath 前缀",
            productionFiles.any { it.projectPath.contains("/core/model/") },
        )
        assertTrue(
            "core/data-contract 选择器匹配 0 文件，检查 projectPath 前缀",
            productionFiles.any { it.projectPath.contains("/core/data-contract/") },
        )
        assertTrue(
            "feature 选择器匹配 0 文件，检查 projectPath 前缀",
            productionFiles.any { it.projectPath.contains("/feature/") },
        )
        assertTrue(
            "core/webview 选择器匹配 0 文件，检查 projectPath 前缀",
            productionFiles.any { it.projectPath.contains("/core/webview/") },
        )
        assertTrue(
            "core/android-platform 选择器匹配 0 文件，检查 projectPath 前缀",
            productionFiles.any { it.projectPath.contains("/core/android-platform/") },
        )
        assertTrue(
            "core/network 选择器匹配 0 文件，检查 projectPath 前缀",
            productionFiles.any { it.projectPath.contains("/core/network/") },
        )
    }

    @Test
    fun `webview container does not expose native callbacks`() {
        val container = productionFiles.single {
            it.projectPath.endsWith("/core/webview/src/main/java/com/example/fragmject/core/webview/WebViewContainer.kt")
        }
        val source = readSource(container)
        val parameters = source.substringAfter("fun WebViewContainer(").substringBefore("\n) {")
        assertTrue("WebViewContainer 参数选择器必须非空", parameters.contains("control: WebViewControl"))
        org.junit.Assert.assertFalse(
            "WebViewContainer 不得通过回调暴露原生 WebView，业务层应使用脚本与图片事件契约",
            Regex("\\b(?:WebView|WebViewClient|WebChromeClient)\\b").containsMatchIn(parameters),
        )
    }

    /**
     * 规则 18：业务成功码只能经 `HttpResponse.isSuccess` / `ApiCode` 判断。
     *
     * 禁止在生产代码里对 `errorCode` 与字符串字面量做 `==` / `!=` 比较（如 `errorCode == "0"`），
     * 否则成功码语义会再次散落到各仓库。唯一允许的比较位于 `HttpResponse.kt`（定义处）。
     */
    @Test
    fun `business success code is judged only through api code`() {
        val literalComparison = Regex("""errorCode\s*(==|!=)\s*"[^"]*"""")
        val definition = "/core/data-contract/src/main/java/com/example/fragmject/core/data/contract/model/HttpResponse.kt"
        val scanned = productionFiles.filterNot { it.projectPath.endsWith(definition) }
        assertTrue("选择器必须覆盖 data-repository 生产文件", scanned.any { it.projectPath.contains("/core/data-repository/") })
        assertTrue("选择器必须覆盖 network 生产文件", scanned.any { it.projectPath.contains("/core/network/") })
        val offenders = scanned
            .filter { literalComparison.containsMatchIn(readSource(it)) }
            .map { it.projectPath }
        assertTrue("以下文件直接比较 errorCode 字面量，请改用 isSuccess / ApiCode：$offenders", offenders.isEmpty())
    }

    /**
     * 规则 19：`core:data-repository` 不得使用 `runCatching`。
     *
     * 仓库里的调用绝大多数是挂起的网络/IO 请求，`runCatching` 会把 `CancellationException`
     * 一并吞掉（曾导致页面退出后积分请求被当成「失败」）。请统一使用
     * `fetchAsDomainResult` / `fetchAsSimpleResult`，或显式 `catch (CancellationException)` 重抛。
     * 注释中的说明文字不计入。
     */
    @Test
    fun `data repository does not use run catching`() {
        val repositoryFiles = productionFiles.filter { it.projectPath.contains("/core/data-repository/") }
        assertTrue("选择器必须覆盖 data-repository 生产文件", repositoryFiles.isNotEmpty())
        val offenders = repositoryFiles.filter { file ->
            readSource(file).lineSequence()
                .map { it.trim() }
                .filterNot { it.startsWith("//") || it.startsWith("*") || it.startsWith("/*") }
                .any { it.contains("runCatching") }
        }.map { it.projectPath }
        assertTrue("以下文件使用了 runCatching，会吞掉协程取消：$offenders", offenders.isEmpty())
    }

    /** 通过项目根目录读取源码文本，兼容 Gradle 以模块目录为工作目录的情况。 */
    private fun readSource(file: KoFileDeclaration): String {
        val root = generateSequence(java.io.File(System.getProperty("user.dir"))) { it.parentFile }
            .first { java.io.File(it, "settings.gradle.kts").isFile }
        return java.io.File(root, file.projectPath.removePrefix("/")).readText()
    }

    // ---- 辅助判定 ----

    /** 判断文件是否属于 main 生产源集（路径包含 `/src/main/`）。 */
    private fun KoFileDeclaration.isMainSource(): Boolean =
        projectPath.contains("/src/main/")

    private fun KoFileDeclaration.isFeatureImpl(): Boolean =
        projectPath.contains("/feature/") && projectPath.contains("/impl/")

    private fun KoFileDeclaration.isFeatureApi(): Boolean =
        projectPath.contains("/feature/") && projectPath.contains("/api/")

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

    /**
     * 判定文件是否 import 了任何 feature impl 类（7+ 段包名）。
     * 用于「feature:*:api 不得依赖 feature:*:impl」规则：api 模块自身不含 impl 子包，
     * 任何 >6 段的 feature 导入都是对 impl 的反向依赖，因此不区分 own
     * （与 [hasOtherFeatureImplImport] 针对 impl 模块区分 own 的语义不同）。
     */
    private fun KoFileDeclaration.hasFeatureImplImport(): Boolean {
        val prefix = "com.example.fragmject.feature."
        return hasImport { imp ->
            imp.name.startsWith(prefix) && imp.name.split(".").size > 6
        }
    }

}
