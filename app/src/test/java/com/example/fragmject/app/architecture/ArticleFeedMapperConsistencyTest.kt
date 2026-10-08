package com.example.fragmject.app.architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.declaration.KoFileDeclaration
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 问题 8 防漂移测试：`Article.toFeedCardUIState` 在 home / collection / user / search
 * 四个 feature impl 中重复实现（core:ui 保持零领域依赖，故未下沉为共享 mapper）。
 *
 * 由于 mapper 依赖 `android.text.Html` 与 `R.mipmap.*`（Android framework），
 * 无法在纯 JVM 单测中直接调用，故改为源码一致性断言：
 * 提取四份 mapper 的 `toFeedCardUIState` 函数体（去空行 / trim），断言完全一致，
 * 一旦某份被单独修改即 fail。
 */
class ArticleFeedMapperConsistencyTest {

    private val productionFiles: List<KoFileDeclaration> = Konsist.scopeFromProject()
        .files
        .filter { it.projectPath.contains("/src/main/") }

    @Test
    fun `article feed card mappers stay consistent`() {
        val mappers = productionFiles.filter {
            it.name == "ArticleFeedMapper" && it.projectPath.contains("/mapper/")
        }

        assertEquals(
            "应存在 4 份 Article.toFeedCardUIState 映射（home/collection/user/search）",
            4,
            mappers.size,
        )

        val bodies = mappers.map { extractMapperBody(it.text) }
        val expected = bodies.first()
        bodies.forEachIndexed { index, body ->
            assertEquals(
                "mapper 漂移：${mappers[index].projectPath} 与首份不一致",
                expected,
                body,
            )
        }
    }

    /** 提取 `fun Article.toFeedCardUIState` 到 `private fun formatChapterName` 之间的映射逻辑。 */
    private fun extractMapperBody(text: String): String {
        val start = text.indexOf("fun Article.toFeedCardUIState")
        val end = text.indexOf("private fun formatChapterName")
        require(start in 0..<end) { "无法定位 mapper 函数体" }
        return text.substring(start, end)
            .lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .joinToString("\n")
    }
}
