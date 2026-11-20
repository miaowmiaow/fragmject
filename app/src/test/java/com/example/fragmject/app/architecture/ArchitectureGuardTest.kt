package com.example.fragmject.app.architecture

import com.example.fragmject.app.architecture.ArchitectureSupport.APP_CORE_AGGREGATE_CAP
import com.example.fragmject.app.architecture.ArchitectureSupport.APP_CORE_AGGREGATE_MODULES
import com.example.fragmject.app.architecture.ArchitectureSupport.APP_DATA_ALLOWLIST_CAP
import com.example.fragmject.app.architecture.ArchitectureSupport.APP_DATA_IMPORT_ALLOWLIST
import com.example.fragmject.app.architecture.ArchitectureSupport.GUARD_RULES_PATH
import com.example.fragmject.app.architecture.ArchitectureSupport.MAX_FILE_LINES
import com.example.fragmject.app.architecture.ArchitectureSupport.WARN_FILE_LINES
import com.example.fragmject.app.architecture.ArchitectureSupport.isFeatureApi
import com.example.fragmject.app.architecture.ArchitectureSupport.isFeatureImpl
import com.example.fragmject.app.architecture.ArchitectureSupport.productionFiles
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 守卫自身的约束：选择器自检、文件体量上限、登记例外的双向一致、allowlist 上限。
 *
 * 从 `ArchitectureTest` 拆出，规则内容与编号保持不变。这一组测试的作用是保证其它
 * 规则不会静默失效（选择器写错即空转），以及守卫与文档不会各自漂移。
 */
class ArchitectureGuardTest {

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
            "core/pet 选择器匹配 0 文件，检查 projectPath 前缀（宠物已拆为 :core:pet）",
            productionFiles.any { it.projectPath.contains("/core/pet/") },
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

    /**
     * 规则 28：单个生产文件不得超过 [MAX_FILE_LINES] 行。
     *
     * 登记这条规则的由来：T-05 一次性拆了 5 个 500~1053 行的文件，若没有守卫，
     * 它们会在几次迭代后重新膨胀。阈值取 500，与当次拆分目标一致；
     * 拆分时不留 baseline/白名单，超了就是超了。
     *
     * 为什么用 `text.lines().size` 而不是声明数量：超长文件的典型成因是注释、KDoc 与
     * 常量表堆积，声明数并不随之增长。
     */
    @Test
    fun `production files stay under five hundred lines`() {
        productionFiles
            .map { it to it.text.lines().size }
            .filter { (_, lines) -> lines in (WARN_FILE_LINES + 1)..MAX_FILE_LINES }
            .forEach { (file, lines) ->
                println("[warn] ${file.projectPath.orEmpty()} 已 $lines 行，接近 $MAX_FILE_LINES 行上限")
            }
        val oversized = productionFiles
            .map { it to it.text.lines().size }
            .filter { (_, lines) -> lines > MAX_FILE_LINES }
            .map { (file, lines) -> "${file.projectPath.orEmpty()} ($lines 行)" }

        assertTrue(
            buildString {
                appendLine("以下生产文件超过 $MAX_FILE_LINES 行，请按职责拆分：")
                oversized.forEach { appendLine("  - $it") }
            },
            oversized.isEmpty(),
        )
    }

    /**
     * 规则 28b：原生源码（`security/src/main/cpp`）同样不得超过 [MAX_FILE_LINES] 行。
     *
     * 规则 28 只扫描 Konsist 可见的 Kotlin 文件，`security.cpp` 曾长到 1782 行而无守卫。
     * 本规则直接读取文件系统。选择器自检：目录或文件缺失即失败，防止路径失效后静默放行。
     */
    @Test
    fun `native sources stay under five hundred lines`() {
        val cppDir = java.io.File("../security/src/main/cpp")
        assertTrue("未找到 C++ 源码目录 ${cppDir.canonicalPath}，规则 28b 选择器失效", cppDir.isDirectory)
        val sources = cppDir.listFiles().orEmpty().filter { it.extension == "cpp" || it.extension == "h" }
        assertTrue("原生源码目录下没有 .cpp/.h 文件，规则 28b 选择器失效", sources.isNotEmpty())

        val counts = sources.associate { it.name to it.readLines().size }
        counts.filter { (_, n) -> n in (WARN_FILE_LINES + 1)..MAX_FILE_LINES }.forEach { (name, n) ->
            println("[warn] 原生源码 $name 已 $n 行，接近 $MAX_FILE_LINES 行上限")
        }
        val oversized = counts.filter { (_, n) -> n > MAX_FILE_LINES }.map { (name, n) -> "$name ($n 行)" }
        assertTrue(
            buildString {
                appendLine("以下原生源码超过 $MAX_FILE_LINES 行，请按职责拆分：")
                oversized.forEach { appendLine("  - $it") }
            },
            oversized.isEmpty(),
        )
    }

    /**
     * 规则 21 的 allowlist 只允许收缩：新增条目即违规（见 `APP_DATA_IMPORT_ALLOWLIST` 注释）。
     * 上限固化为常量，而不是只靠注释约定。
     */
    @Test
    fun `app data import allowlist only shrinks`() {
        val entries = APP_DATA_IMPORT_ALLOWLIST.values.sumOf { it.size }
        assertTrue(
            "规则 21 allowlist 条目数为 $entries，上限 $APP_DATA_ALLOWLIST_CAP。只允许减少，不允许新增。",
            entries <= APP_DATA_ALLOWLIST_CAP,
        )
    }

    /**
     * 规则 31b 的 allowlist 只允许收缩：新增条目即违规（见 `APP_CORE_AGGREGATE_MODULES` 注释）。
     *
     * 没有这条上限，`APP_CORE_AGGREGATE_MODULES` 就退化成「自我豁免名单」——
     * 往集合里加一行即可绕过 §6.6 登记，规则 31b 会永远通过。
     */
    @Test
    fun `app core aggregate allowlist only shrinks`() {
        val entries = APP_CORE_AGGREGATE_MODULES.size
        assertTrue(
            "规则 31b 聚合白名单条目数为 $entries，上限 $APP_CORE_AGGREGATE_CAP。" +
                "新增能力型 core 边请登记到 dependencies-policy.md §6.6，不要往这个集合里加。",
            entries <= APP_CORE_AGGREGATE_CAP,
        )
    }

    /**
     * 规则 31：core→core 登记例外与 `dependencies-policy.md` §6.6 双向一致。
     *
     * 例外边的唯一来源是守卫文件中的 `CORE_EXCEPTION_EDGES`；守卫只从「常规边 ∪ 例外」的合并结果判定，
     * 因此新增 core 例外必然经过该集合。
     * - 正向：每条例外边在 §6.6 有一行同时引用两端模块的登记；未登记即失败。
     * - 反向：§6.6 中以「`core:A` 依赖 `core:B`」声明的边必须是守卫中的例外；残留的已删除例外即失败。
     *
     * 这是文本级检查（读取守卫源码与文档），与规则 21 的白名单做法一致。
     * 选择器自检：解析结果为空即失败，防止正则或路径失效后静默放行。
     */
    @Test
    fun `core exception edges are registered in dependencies policy both ways`() {
        val guardSource = java.io.File(GUARD_RULES_PATH).readText()
        val block = guardSource
            .substringAfter("CORE_EXCEPTION_EDGES: Set<Pair<String, String>> = setOf(", "")
            .substringBefore("\n)")
        val edges = Regex("\"(:core:[a-z-]+)\"\\s*to\\s*\"(:core:[a-z-]+)\"")
            .findAll(block)
            .map { it.groupValues[1] to it.groupValues[2] }
            .toSet()
        assertTrue("未从守卫解析到 CORE_EXCEPTION_EDGES，规则 31 选择器失效", edges.isNotEmpty())

        val policy = java.io.File("../docs/dependencies-policy.md").readText()
        val section = policy.substringAfter("### 6.6 例外管理", "").substringBefore("\n### ")
        val rows = section.lines().filter { it.startsWith("|") }
        assertTrue("dependencies-policy.md §6.6 表格未找到，规则 31 选择器失效", rows.isNotEmpty())

        // 守卫中模块名带前导冒号（:core:ui），文档中写作 `core:ui`，匹配前统一去掉冒号
        val unregistered = edges.filterNot { (from, to) ->
            val a = "`${from.removePrefix(":")}`"
            val b = "`${to.removePrefix(":")}`"
            rows.any { a in it && b in it }
        }
        assertTrue(
            "守卫登记例外未在 dependencies-policy.md §6.6 登记，请补登记行：$unregistered",
            unregistered.isEmpty(),
        )

        val declaredInDoc = rows.flatMap { row ->
            val subject = row.split("|").getOrNull(1).orEmpty()
            if ("依赖" !in subject) return@flatMap emptyList()
            val names = Regex("`(core:[a-z-]+)`").findAll(subject).map { ":" + it.groupValues[1] }.toList()
            if (names.size < 2) emptyList() else names.drop(1).map { names.first() to it }
        }.toSet()
        val stale = declaredInDoc - edges
        assertTrue(
            "dependencies-policy.md §6.6 声明了守卫中不存在的 core 例外（残留或未同步）：$stale",
            stale.isEmpty(),
        )
    }

    /**
     * 规则 31b：`:app` → `core:*` 的**非聚合**边必须登记在 `dependencies-policy.md` §6.6。
     *
     * 规则 31 只覆盖 core→core 例外，app 侧新增的边没有登记流程——`app → core:pet` 一度只能靠
     * `layering.md` 的一句说明和守卫注释，两处文档各写一半，删了注释也没人发现。
     *
     * 判定方式：从守卫源码解析 `APP_ALLOWED_DEPENDENCIES`，减去「数据 / 运行时聚合」白名单
     * （这些边是 Hilt 聚合与导航装配的必需品，无需逐条写理由），剩下的每条 `:core:x` 边
     * 都必须在 §6.6 有一行登记。
     *
     * 新增聚合模块时把模块加进 [APP_CORE_AGGREGATE_MODULES] 即可；新增**能力型** core 边
     * （如装饰层、引擎）则应写进 §6.6，说明「为什么不用契约」。
     */
    @Test
    fun `app core edges are either aggregates or registered in dependencies policy`() {
        val guardSource = java.io.File(GUARD_RULES_PATH).readText()
        val block = guardSource
            .substringAfter("APP_ALLOWED_DEPENDENCIES: Set<String> = setOf(", "")
            .substringBefore("\n)")
        val appCoreEdges = Regex("\"(:core:[a-z-]+)\"")
            .findAll(block)
            .map { it.groupValues[1] }
            .toSet()
        assertTrue("未从守卫解析到 APP_ALLOWED_DEPENDENCIES 的 core 边，规则 31b 选择器失效", appCoreEdges.isNotEmpty())

        val policy = java.io.File("../docs/dependencies-policy.md").readText()
        val section = policy.substringAfter("### 6.6 例外管理", "").substringBefore("\n### ")
        val rows = section.lines().filter { it.startsWith("|") }
        assertTrue("dependencies-policy.md §6.6 表格未找到，规则 31b 选择器失效", rows.isNotEmpty())

        val unregistered = appCoreEdges
            .filterNot { it in APP_CORE_AGGREGATE_MODULES }
            .filterNot { coreEdgeName ->
                val name = "`${coreEdgeName.removePrefix(":")}`"
                rows.any { "`app`" in it && name in it }
            }
        assertTrue(
            "app→core 的能力型依赖未在 dependencies-policy.md §6.6 登记：$unregistered。" +
                "若是数据/Hilt 聚合模块，请加入 APP_CORE_AGGREGATE_MODULES 并说明理由。",
            unregistered.isEmpty(),
        )
    }
}
