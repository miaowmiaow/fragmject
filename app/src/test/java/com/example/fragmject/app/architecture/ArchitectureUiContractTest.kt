package com.example.fragmject.app.architecture

import com.example.fragmject.app.architecture.ArchitectureSupport.isFeatureImpl
import com.example.fragmject.app.architecture.ArchitectureSupport.productionFiles
import com.example.fragmject.app.architecture.ArchitectureSupport.readSource
import com.lemonappdev.konsist.api.verify.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * UI 与导航契约（ViewModel 创建方式、导航契约完整性、颜色字面量、WebView 容器参数）。
 *
 * 从 `ArchitectureTest` 拆出，规则内容与编号保持不变。
 */
class ArchitectureUiContractTest {

    /**
     * 规则 14：feature 内创建 ViewModel 必须使用 `hiltViewModel()`。
     *
     * 项目已接入 entry 级 ViewModelStore（`rememberViewModelStoreNavEntryDecorator`），
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
     * 规则 20：导航契约必须完整接入「聚合容器 + app 实现」，且接口方法不得带默认实现。
     *
     * 契约方法一旦带 `= Unit` 默认实现，新增语义动作时漏改 `AppXxxNavigator` 也能编译通过，
     * 运行时表现为「点击无反应」。因此：
     * - 契约接口不得写 `= Unit`（漏实现直接编译失败，覆盖 app 实现与 [Navigators.noop]）；
     * - 每个 `*Navigator` 都必须在 `Navigators` 聚合体与 `noop()` 中有对应字段
     *   （漏字段不会被编译器发现，只能由本规则兜底）；
     * - `AppNavigators.kt` 必须有对应实现类，且经 `@Binds` 装配（装配由 Hilt 编译期校验）。
     */
    @Test
    fun `navigator contract is fully wired`() {
        val contracts = productionFiles.filter {
            it.projectPath.contains("/core/navigation-contract/") && it.projectPath.endsWith("Navigator.kt")
        }
        assertTrue("契约选择器匹配 0 文件，检查 core/navigation-contract 路径", contracts.size >= 9)
        val locals = readSource(
            productionFiles.single { it.projectPath.endsWith("/Navigators.kt") },
        )
        val appNavigators = readSource(
            productionFiles.single { it.projectPath.endsWith("/AppNavigators.kt") },
        )
        contracts.forEach { file ->
            val contract = file.projectPath.substringAfterLast("/").removeSuffix(".kt")
            val field = contract.removeSuffix("Navigator").replaceFirstChar { it.lowercase() }
            val source = readSource(file)
            org.junit.Assert.assertFalse(
                "$contract 的方法不得带默认空实现（去掉 `= Unit`，让漏实现变成编译错误）",
                Regex("""fun \w+\([^)]*\) *= *Unit""").containsMatchIn(source),
            )
            assertTrue(
                "Navigators 聚合体缺少 `$field: $contract` 字段（新增契约需同步 Navigators + noop）",
                Regex("""val $field: $contract\b""").containsMatchIn(locals),
            )
            assertTrue(
                "Navigators.noop() 缺少 $contract 的空实现",
                Regex("""$field = object : $contract\b""").containsMatchIn(locals),
            )
            assertTrue(
                "AppNavigators.kt 缺少 App${contract.removeSuffix("Navigator")}Navigator : $contract 实现",
                Regex("""class App${contract.removeSuffix("Navigator")}Navigator\b[\s\S]*?: $contract\b""")
                    .containsMatchIn(appNavigators),
            )
        }
    }

    /**
     * 规则 24：`feature:*:impl` 的 UI 代码不得硬编码颜色字面量。
     *
     * 颜色一律进 `AppColors`（dependencies-policy.md §6.7）：散落的 `Color(0xFF...)` 会让主题
     * 切换与暗色适配失效，且同一色值在多个 feature 重复定义后无法统一调整。2026-10 评审时
     * `feature/picture/impl` 有 8 处硬编码（`0xFF508CEE` / `0xFF2B2B2B` / `0xFF333333`），
     * 已收敛为 `AppColors.blue` / `AppColors.menuContainer` / `AppColors.mediaPlaceholder`。
     *
     * 放行两类（都是「集中定义」而非「散落使用」）：
     * - 文件名以 `Colors.kt` / `Theme.kt` 结尾：本身就是颜色定义处（如 `AuthenticatorColors`）；
     * - `/feature/demo/`：平台 API 演示模块，与规则 13 同理，业务 feature 不得效仿。
     *
     * 存量例外（不在本规则扫描范围，但**禁止新增**）：`core:ui` 的 `NightSwitchButton` 是
     * Canvas 自绘装饰色（太阳/月亮/星空渐变），与主题令牌无关，不适合进 `AppColors`。
     */
    @Test
    fun `feature impl does not hardcode colors`() {
        val colorLiteral = Regex("Color\\(0x[0-9A-Fa-f]{6,8}\\)")
        val scanned = productionFiles
            .filter { it.isFeatureImpl() }
            .filterNot { it.projectPath.contains("/feature/demo/") }
            .filterNot {
                val name = it.projectPath.substringAfterLast('/')
                name.endsWith("Colors.kt") || name.endsWith("Theme.kt")
            }
        assertTrue("feature impl 选择器匹配 0 文件（选择器写错会让本规则空转）", scanned.isNotEmpty())
        val offenders = scanned
            .filter { colorLiteral.containsMatchIn(readSource(it)) }
            .map { it.projectPath }
        assertTrue(
            "feature:*:impl 不得硬编码 Color(0x...)（$offenders）——一律进 AppColors，或集中到 *Colors.kt",
            offenders.isEmpty(),
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
}
