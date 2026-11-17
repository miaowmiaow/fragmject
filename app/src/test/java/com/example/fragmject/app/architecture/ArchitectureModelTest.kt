package com.example.fragmject.app.architecture

import com.example.fragmject.app.architecture.ArchitectureSupport.productionFiles
import com.example.fragmject.app.architecture.ArchitectureSupport.readSource
import com.lemonappdev.konsist.api.verify.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 共享协议模型与业务码（DTO 变体、同名模型复制、errorCode 字面量比较）。
 *
 * 从 `ArchitectureTest` 拆出，规则内容与编号保持不变。
 */
class ArchitectureModelTest {

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
     * 规则 29：禁止为共享协议模型新增 DTO 变体类（`*Dto` / `*DTO`）。
     *
     * 依据 `docs/adr/0001-shared-protocol-model.md`：`:core:domain` 只允许依赖
     * `:core:model`（`CORE_ALLOWED_DEPENDENCIES`），把 DTO 移出 model 后，7 个领域端口
     * `Flow<PagingData<Article>>` 会立刻断裂，唯一出路是在 domain 复制一份同构模型——
     * 即「双份 36 字段 + 逐字段 mapper」。
     *
     * 本条规则的作用不是禁止讨论，而是把「重新提案」从口头讨论变成一次**显式变更**：
     * 想加 DTO，必须先改这条规则，并在 PR 里按 ADR §5 的触发条件举证。
     * 现有代码零命中（无 baseline 负担），命中即说明决议被绕过。
     */
    @Test
    fun `no dto variants of shared protocol models`() {
        productionFiles.assertFalse(
            additionalMessage = "禁止新增 *Dto/*DTO 类：core:model 是共享协议模型唯一落点；" +
                "确需拆分请先满足 docs/adr/0001-shared-protocol-model.md §5 的触发条件并更新本规则",
        ) { file ->
            file.classes().any { klass ->
                klass.name.endsWith("Dto") || klass.name.endsWith("DTO")
            }
        }
    }

    /**
     * 规则 30：不得在 `core:model` 之外声明与共享协议模型同名的类。
     *
     * 规则 29 拦的是「加后缀」（`*Dto` / `*DTO`），本规则拦的是「原样复制」——
     * 即 ADR-0001 §3 的 B2 方案（在 `core:domain` 自建 `Article`）。只堵后缀，改名成
     * `ArticleWire` / `RemoteArticle` 就能绕过；两条规则合起来，把「拆模型」从改名游戏
     * 变成一次必须面对 ADR §5 的显式变更。
     *
     * 允许 `*Entity` 这类持久化映射类：名字不同名（`UserEntity` ≠ `User`），且映射目标是
     * SQLite schema 而非协议形状，属于 data 层应有的差异，与「协议模型复制」无关。
     */
    @Test
    fun `no duplicated model names outside core model`() {
        val modelNames = productionFiles
            .filter { it.projectPath.contains("/core/model/") }
            .flatMap { file -> file.classes().map { klass -> klass.name } }
            .toSet()

        // 防空转：路径选择器一旦失效，modelNames 为空 → 下面的断言恒真，守卫静默失效。
        // 用 Article 这个必然存在的锚点做自检，比断言 size > 0 更不容易被重构破坏。
        assertTrue(
            "未解析到 core:model 的模型类声明，规则 30 选择器失效（modelNames 为空）",
            "Article" in modelNames,
        )

        productionFiles
            .filterNot { it.projectPath.contains("/core/model/") }
            .assertFalse(
                additionalMessage = "禁止在 core:model 之外声明同名模型类（ADR-0001 §3 B2 已否决）；" +
                    "持久化映射请用 *Entity 后缀，协议差异请说明为何命中 ADR §5 触发条件",
            ) { file ->
                file.classes().any { klass -> klass.name in modelNames }
            }
    }
}
