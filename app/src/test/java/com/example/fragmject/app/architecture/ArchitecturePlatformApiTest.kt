package com.example.fragmject.app.architecture

import com.example.fragmject.app.architecture.ArchitectureSupport.isUiLayerFile
import com.example.fragmject.app.architecture.ArchitectureSupport.productionFiles
import com.example.fragmject.app.architecture.ArchitectureSupport.readSource
import com.lemonappdev.konsist.api.verify.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Android 框架与平台 API 的使用边界（framework 白名单、WebView、CookieStore、run catching）。
 *
 * 从 `ArchitectureTest` 拆出，规则内容与编号保持不变。
 */
class ArchitecturePlatformApiTest {

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
     * 规则 11：UI 层（`core:ui` / `core:pet`）仅禁止数据 / 媒体提供类 API 混入。
     * UI 组件库允许 BitmapFactory / android.text.Html / displayMetrics 等合法 UI 能力，
     * 但禁止 android.provider.* / android.content.ContentResolver / android.media.* / android.database.*，
     * 防止媒体查询或数据库代码误放进 UI 组件库。
     *
     * 选择器用 [isUiLayerFile]（同时匹配 `core:ui` 与 `core:pet`）：宠物拆出独立模块后，
     * 若只按 `/core/ui/` 过滤，这条规则会对宠物静默失效。
     */
    @Test
    fun `ui layer does not depend on data provider apis`() {
        productionFiles
            .filter { it.isUiLayerFile() }
            .assertFalse(
                additionalMessage = "UI 层（core:ui / core:pet）不得依赖 android.provider.* / " +
                    "android.content.ContentResolver / android.media.* / android.database.*",
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
     * core:testing 虽属 main 源集，但只被各模块 testImplementation 引用、不进生产包，
     * 其共享的 FakeCookieStore 是测试替身而非生产实现，故同样排除。
     */
    @Test
    fun `cookie store implementation only in core webview`() {
        productionFiles
            .filter { it.projectPath.contains("/core/") }
            .filterNot { it.projectPath.contains("/core/webview/") }
            .filterNot { it.projectPath.contains("/core/testing/") }
            .assertFalse(
                additionalMessage = "CookieStore 实现只允许出现在 core:webview（唯一实现 WebViewCookieStore），其他 core 模块只允许注入使用",
            ) { file ->
                file.classes().any { klass ->
                    klass.countParents { parent -> parent.name == "CookieStore" } > 0
                }
            }
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
}
