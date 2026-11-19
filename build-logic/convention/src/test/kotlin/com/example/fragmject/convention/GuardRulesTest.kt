package com.example.fragmject.convention

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 依赖守卫判定规则（[checkRule] / [detectCycles] / 核心白名单）的纯 JVM 单测。
 *
 * 这些规则只处理模块路径字符串，与 Gradle 图无关；覆盖每条规则的正例与反例，
 * 防止白名单或分支在重构中被悄悄改变。
 */
class GuardRulesTest {

    // ---- 单条边判定：checkRule ----

    @Test
    fun `feature impl 依赖白名单内的 core 允许`() {
        assertNull(checkRule(":feature:auth:impl", ":core:domain"))
    }

    @Test
    fun `feature impl 依赖白名单外的 core 违规（数据实现层）`() {
        assertNotNull(checkRule(":feature:auth:impl", ":core:database"))
    }

    @Test
    fun `feature impl 依赖其他 feature 的 impl 违规`() {
        val message = checkRule(":feature:auth:impl", ":feature:user:impl")
        assertNotNull(message)
        assertTrue(message!!.contains("不得依赖其他 feature impl"))
    }

    @Test
    fun `feature impl 依赖其他 feature 的 api 违规`() {
        val message = checkRule(":feature:auth:impl", ":feature:user:api")
        assertNotNull(message)
        assertTrue(message!!.contains("不得依赖其他 feature api"))
    }

    @Test
    fun `feature impl 依赖本 feature 自己的 api 允许`() {
        assertNull(checkRule(":feature:auth:impl", ":feature:auth:api"))
    }

    @Test
    fun `feature api 之间横向依赖违规`() {
        val message = checkRule(":feature:auth:api", ":feature:user:api")
        assertNotNull(message)
        assertTrue(message!!.contains("契约层禁止横向依赖"))
    }

    @Test
    fun `feature api 依赖 impl 违规`() {
        val message = checkRule(":feature:auth:api", ":feature:auth:impl")
        assertNotNull(message)
        assertTrue(message!!.contains("feature api 不得依赖 feature impl"))
    }

    @Test
    fun `feature api 只允许依赖 navigation-contract`() {
        assertNull(checkRule(":feature:auth:api", ":core:navigation-contract"))
        assertNotNull(checkRule(":feature:auth:api", ":core:domain"))
    }

    @Test
    fun `core 不得依赖 app，feature 也不得依赖 app`() {
        assertNotNull(checkRule(":core:model", ":app"))
        assertNotNull(checkRule(":feature:auth:impl", ":app"))
    }

    @Test
    fun `core 反向依赖 feature 违规`() {
        assertNotNull(checkRule(":core:ui", ":feature:auth:impl"))
    }

    @Test
    fun `domain 依赖数据层违规`() {
        assertNotNull(checkRule(":core:domain", ":core:network"))
        assertNotNull(checkRule(":core:domain", ":core:database"))
        assertNotNull(checkRule(":core:domain", ":core:data-contract"))
        assertNull(checkRule(":core:domain", ":core:model"))
    }

    @Test
    fun `data-repository 不得依赖 network 与 database`() {
        assertNotNull(checkRule(":core:data-repository", ":core:database"))
        assertNotNull(checkRule(":core:data-repository", ":core:network"))
        assertNull(checkRule(":core:data-repository", ":core:data-contract"))
    }

    @Test
    fun `app 只允许依赖组合根白名单内的模块`() {
        assertNull(checkRule(":app", ":feature:auth:impl"))
        assertNotNull(checkRule(":app", ":feature:not-a-real:impl"))
    }

    @Test
    fun `core 常规允许边通过，未声明的 core 边违规`() {
        assertNull(checkRule(":core:domain", ":core:model"))
        assertNotNull(checkRule(":core:model", ":core:domain"))
    }

    @Test
    fun `core 登记例外边通过（合并进了实际矩阵）`() {
        // 这些边不在 CORE_BASE_DEPENDENCIES 中，只有合并登记例外后才允许
        assertNull(checkRule(":core:ui", ":core:model"))
        assertNull(checkRule(":core:ui", ":core:domain"))
        assertNull(checkRule(":core:network", ":core:domain"))
        assertNull(checkRule(":core:testing", ":core:domain"))
        assertNull(checkRule(":core:database", ":core:domain"))
    }

    @Test
    fun `core 例外不会放行例外之外的边`() {
        assertNotNull(checkRule(":core:ui", ":core:network"))
        assertNotNull(checkRule(":core:network", ":core:database"))
    }

    // ---- 登记例外与合并矩阵的一致性 ----

    @Test
    fun `每条登记例外都已合并进实际允许矩阵`() {
        for ((from, to) in CORE_EXCEPTION_EDGES) {
            assertTrue("$from -> $to 未进入 CORE_ALLOWED_DEPENDENCIES", CORE_ALLOWED_DEPENDENCIES[from].orEmpty().contains(to))
        }
    }

    @Test
    fun `合并矩阵保留了常规边，且例外不影响它们`() {
        for ((from, tos) in CORE_BASE_DEPENDENCIES) {
            assertTrue("$from 的常规边丢失", CORE_ALLOWED_DEPENDENCIES[from].orEmpty().containsAll(tos))
        }
    }

    // ---- 依赖环：detectCycles ----

    @Test
    fun `无环图没有报告`() {
        val edges = mapOf(
            ":a" to setOf(":b"),
            ":b" to setOf(":c"),
            ":c" to emptySet(),
        )
        assertTrue(detectCycles(edges).isEmpty())
    }

    @Test
    fun `两节点互相依赖被报告为环`() {
        val edges = mapOf(
            ":a" to setOf(":b"),
            ":b" to setOf(":a"),
        )
        val cycles = detectCycles(edges)
        assertEquals(1, cycles.size)
        assertTrue(cycles.single().contains(":a"))
        assertTrue(cycles.single().contains(":b"))
        assertTrue(cycles.single().contains("->"))
    }

    @Test
    fun `自环也被报告`() {
        val cycles = detectCycles(mapOf(":a" to setOf(":a")))
        assertFalse(cycles.isEmpty())
    }

    // ---- 隐式外部依赖：androidx.core 子包解析与判定 ----

    @Test
    fun `解析 androidx core 的子包名`() {
        assertEquals("net", parseCoreImportPackage("import androidx.core.net.toUri"))
        assertEquals("splashscreen", parseCoreImportPackage("import androidx.core.splashscreen.SplashScreen"))
    }

    @Test
    fun `非 androidx core 的 import 解析为 null`() {
        assertNull(parseCoreImportPackage("import androidx.compose.ui.Modifier"))
        assertNull(parseCoreImportPackage("val x = 1"))
    }

    @Test
    fun `缩进的 import 也能解析`() {
        assertEquals("view", parseCoreImportPackage("    import androidx.core.view.isVisible"))
    }

    @Test
    fun `普通子包所需构件为 core-ktx 或 core`() {
        assertEquals(setOf("core-ktx", "core"), requiredCoreArtifacts("net"))
    }

    @Test
    fun `splashscreen 所需构件为 core-splashscreen`() {
        assertEquals(setOf("core-splashscreen"), requiredCoreArtifacts("splashscreen"))
    }

    @Test
    fun `已声明所需构件之一则不算未满足`() {
        assertTrue(unmetCoreImports(setOf("net"), setOf("core-ktx")).isEmpty())
        assertTrue(unmetCoreImports(setOf("net"), setOf("core")).isEmpty())
    }

    @Test
    fun `只声明 core-ktx 不满足 splashscreen`() {
        val unmet = unmetCoreImports(setOf("splashscreen"), setOf("core-ktx"))
        assertEquals(1, unmet.size)
        assertEquals("splashscreen", unmet.single().first)
        assertEquals(setOf("core-splashscreen"), unmet.single().second)
    }

    @Test
    fun `未使用任何 core 子包时没有未满足项`() {
        assertTrue(unmetCoreImports(emptySet(), emptySet()).isEmpty())
    }

    @Test
    fun `同时使用多个子包时只报告未满足的那个`() {
        val unmet = unmetCoreImports(setOf("net", "splashscreen"), setOf("core-ktx"))
        assertEquals(listOf("splashscreen"), unmet.map { it.first })
    }
}
