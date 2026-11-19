package com.example.fragmject.convention

/**
 * 依赖守卫的纯判定规则：白名单、单条边的合法性、依赖环。
 *
 * 与 Gradle 无关（只处理模块路径字符串），因此可在 JVM 单测中直接覆盖。
 * [FragmjectAndroidDependencyGuardPlugin] 负责从 Gradle 依赖图构建边，并把边交给这里判定。
 */

/**
 * app 组合根允许依赖的模块白名单（正向声明）。
 *
 * app 是唯一组合根：依赖全部 feature:*:impl 以注册其 NavContent/Hilt 入口，
 * 并依赖 Hilt 聚合所需的 core 模块（database/data-repository/network 等）。
 * 未声明的 app→x 边一律失败，防止引入不必要的聚合依赖。
 */
internal val APP_ALLOWED_DEPENDENCIES: Set<String> = setOf(
    // 运行期安全防护库：独立交付能力，不从 :core 走
    ":security",
    // 不含 :core:android-platform：平台模块只被 core 内部消费，
    // app 与 feature 一律经 core:domain 端口间接使用（未声明即失败，等价于一条反向规则）
    ":core:data-repository",
    ":core:database",
    ":core:designsystem",
    ":core:domain",
    ":core:navigation-runtime",
    ":core:navigation-contract",
    ":core:network",
    ":core:webview",
    ":feature:article:impl",
    ":feature:authenticator:impl",
    ":feature:auth:impl",
    ":feature:collection:impl",
    ":feature:demo:impl",
    ":feature:home:impl",
    ":feature:search:impl",
    ":feature:user:impl",
    ":feature:picture:impl",
    // 契约层（NavKey）：组合根装配导航图需要，必须显式声明。
    // 各 impl 对自身 api 已降为 implementation，不再经 api() 传递到这里，
    // 因此这两组边都会被本白名单真实检查（守卫只收集直接依赖边）。
    ":feature:article:api",
    ":feature:authenticator:api",
    ":feature:auth:api",
    ":feature:collection:api",
    ":feature:demo:api",
    ":feature:home:api",
    ":feature:search:api",
    ":feature:user:api",
    ":feature:picture:api",
)

/**
 * core→core 登记例外（边的字面量）。每一条都必须在 `dependencies-policy.md` §6.6 有登记行，
 * 由 ArchitectureTest 规则 31 双向校验：新增例外未登记即失败；§6.6 中残留的已删除例外也失败。
 *
 * 常规允许边在 [CORE_BASE_DEPENDENCIES]；守卫实际使用的矩阵由两者合并得到（见 [CORE_ALLOWED_DEPENDENCIES]），
 * 因此任何 core 例外都必须经过本集合，无法绕过登记。
 */
internal val CORE_EXCEPTION_EDGES: Set<Pair<String, String>> = setOf(
    // database 直接实现 domain 端口（History/Schedule/Theme/OtpAccount 均无中间透传层），
    // domain 为纯契约层（仅依赖 model），数据实现依赖领域抽象方向正确。
    ":core:database" to ":core:domain",
    // network 依赖 domain 仅为引用 CookieStore 这一共享会话契约（WebView 登录后由 OkHttp 复用）。
    // network 不得因此依赖 webview；实现唯一在 core:webview（经 Hilt @Binds 绑定），network 只允许注入使用。
    // 规则 15 在源码层二次拦截 network 内出现 CookieStore 实现类。
    ":core:network" to ":core:domain",
    // core:ui 依赖 model / domain 是登记过的例外（2026-10 边界评审）：
    // ArticleFeedMapper 展示领域模型；CollectInteraction 引用 domain 的 CollectState 契约。
    // 只允许这两条例外边，且业务文案/图标必须参数化可覆盖，不得下探到 data-repository / network / database。
    ":core:ui" to ":core:model",
    ":core:ui" to ":core:domain",
    // 测试工具模块：认证用例的共享 fake 需要领域端口类型，只被各模块 testImplementation 消费，不进生产包。
    ":core:testing" to ":core:domain",
)

/** core 模块间的常规允许边（不含登记例外）。未声明的 core→core 边一律失败。 */
internal val CORE_BASE_DEPENDENCIES: Map<String, Set<String>> = mapOf(
    ":core:model" to emptySet(),
    ":core:android-platform" to emptySet(),
    ":core:navigation-contract" to emptySet(),
    ":core:navigation-runtime" to emptySet(),
    ":core:designsystem" to emptySet(),
    ":core:data-contract" to setOf(":core:model"),
    ":core:domain" to setOf(":core:model"),
    // android-platform：OTP 密钥落库前需 Keystore 静态加密（KeystoreSecretCipher），
    // 与「加密必须与存储同层」一致——data-repository 可用 android-platform 但不可见 DAO。
    ":core:database" to setOf(":core:android-platform", ":core:data-contract", ":core:model"),
    ":core:network" to setOf(":core:android-platform", ":core:data-contract", ":core:model"),
    ":core:data-repository" to setOf(
        ":core:domain",
        ":core:android-platform",
        ":core:data-contract",
        ":core:model",
    ),
    ":core:ui" to setOf(":core:designsystem"),
    ":core:image-editor" to setOf(":core:domain", ":core:android-platform"),
    // qrscanner 只依赖 CameraX / ML Kit 等外部 SDK，不依赖其他 core 模块：
    // 这些 SDK 由本引擎独占持有，feature 不得各自引入一份。
    ":core:qrscanner" to emptySet(),
    ":core:webview" to setOf(":core:android-platform", ":core:domain"),
    ":core:testing" to emptySet(),
)

/** 守卫实际使用的 core 允许矩阵：常规边 ∪ 登记例外。语义与合并前的单一 map 完全一致。 */
internal val CORE_ALLOWED_DEPENDENCIES: Map<String, Set<String>> =
    mergeCoreEdges(CORE_BASE_DEPENDENCIES, CORE_EXCEPTION_EDGES)

private fun mergeCoreEdges(
    base: Map<String, Set<String>>,
    exceptions: Set<Pair<String, String>>,
): Map<String, Set<String>> {
    val merged = mutableMapOf<String, MutableSet<String>>()
    base.forEach { (from, tos) -> merged.getOrPut(from) { mutableSetOf() }.addAll(tos) }
    exceptions.forEach { (from, to) -> merged.getOrPut(from) { mutableSetOf() }.add(to) }
    return merged.mapValues { it.value.toSet() }
}

/**
 * feature 模块允许依赖的 core 模块白名单（正向声明）。
 *
 * feature 只能经由 :core:domain 端口访问数据，因此 data-contract / data-repository /
 * network / database 一律不在白名单内，防止数据实现层泄漏到 UI 层。
 * 未声明的 feature→core 边一律失败。
 *
 * 白名单里的 :core:webview / :core:qrscanner / :core:image-editor 属
 * **引擎模块**（dependencies-policy.md §6.10）：它们持有 Android platform API 或第三方 SDK
 * （webkit / CameraX+ML Kit / 位图+Canvas），且以 UI 形态存在，无法用 domain 端口表达，
 * 因此允许 feature 直接依赖。
 *
 * ⚠️ 判据提醒：**不要用「当前只有 1 个消费者」去否决引擎模块**。引擎化的目的是消灭平台类型泄漏
 * 与 domain 端口污染（历史上的 MediaEditor / ImageHandle / ImageSource 三个端口即因此被
 * :core:image-editor 取代），不是共享复用——消费者数量是结果，不是判据。
 * :core:image-editor 目前仅 picture/impl 消费属预期，不要把它下沉回 feature。
 */
internal val FEATURE_ALLOWED_CORE_DEPENDENCIES: Set<String> = setOf(
    ":core:designsystem",
    ":core:domain",
    ":core:image-editor",
    ":core:model",
    ":core:navigation-runtime",
    ":core:navigation-contract",
    ":core:qrscanner",
    ":core:ui",
    ":core:webview",
)

/**
 * feature:*:api 允许依赖的 core 模块白名单（正向声明）。
 *
 * api 是纯契约层（NavKey 路由键），只允许依赖 navigation-contract；
 * 运行时机制（navigation-runtime）、领域端口（domain）、UI（ui/designsystem）
 * 一律不得进入 api。外部库（如 androidx.navigation3.runtime）不受本表约束。
 */
internal val FEATURE_API_ALLOWED_CORE_DEPENDENCIES: Set<String> = setOf(
    ":core:navigation-contract",
)

internal fun checkRule(from: String, to: String): String? {
    // 新增：app 组合根只允许依赖白名单内的模块（正向声明），
    // 防止引入不必要的聚合边（如仅 Hilt 聚合、无源码引用却显式依赖的模块）
    if (from == ":app" && to !in APP_ALLOWED_DEPENDENCIES) {
        return "$from -> $to（app 依赖超出组合根白名单）"
    }
    // R7: core / feature 不得依赖 :app
    if ((from.startsWith(":core:") || from.startsWith(":feature:")) && to == ":app") {
        return "$from -> $to（core/feature 不得依赖 :app）"
    }
    // R4: core 不得反向依赖 feature
    if (from.startsWith(":core:") && to.startsWith(":feature:")) {
        return "$from -> $to（core 不得反向依赖 feature）"
    }
    // R1: domain 不得依赖 data-contract / data-repository / network / database
    if (from == ":core:domain" && to in setOf(
            ":core:data-contract",
            ":core:data-repository",
            ":core:network",
            ":core:database",
        )
    ) {
        return "$from -> $to（domain 不得依赖 data/network/database）"
    }
    // R5: data-repository 不得依赖 network / database
    if (from == ":core:data-repository" && to in setOf(":core:network", ":core:database")) {
        return "$from -> $to（data-repository 不得依赖 network/database）"
    }
    // R6: feature:*:api 不得依赖任何 feature:*:impl
    if (from.startsWith(":feature:") && from.endsWith(":api") &&
        to.startsWith(":feature:") && to.endsWith(":impl")
    ) {
        return "$from -> $to（feature api 不得依赖 feature impl）"
    }
    // R2 / R3: feature:*:impl 不得依赖其他 feature 的 impl / api
    if (from.startsWith(":feature:") && from.endsWith(":impl")) {
        val fromFeature = from.removePrefix(":feature:").substringBefore(":")
        if (to.startsWith(":feature:") && to != from) {
            val toFeature = to.removePrefix(":feature:").substringBefore(":")
            if (toFeature != fromFeature) {
                if (to.endsWith(":impl")) {
                    return "$from -> $to（feature impl 不得依赖其他 feature impl）"
                }
                if (to.endsWith(":api")) {
                    return "$from -> $to（feature impl 不得依赖其他 feature api）"
                }
            }
        }
    }
    // R10: feature:*:api 不得依赖其他 feature:*:api（契约层横向边）。
    // R2/R3 的前置条件是「from 以 :impl 结尾」，R6 只禁 api→impl，
    // 因此 api→api 此前是 Gradle 守卫与 Konsist 规则的交叉盲区，这里补上。
    if (from.startsWith(":feature:") && from.endsWith(":api") &&
        to.startsWith(":feature:") && to.endsWith(":api")
    ) {
        val fromFeature = from.removePrefix(":feature:").substringBefore(":")
        val toFeature = to.removePrefix(":feature:").substringBefore(":")
        if (toFeature != fromFeature) {
            return "$from -> $to（feature api 不得依赖其他 feature api：契约层禁止横向依赖）"
        }
    }
    // R8: feature 依赖的 core 模块必须在白名单内（正向声明，未声明的 feature→core 边一律失败）
    if (from.startsWith(":feature:") && to.startsWith(":core:")) {
        val allowed = FEATURE_ALLOWED_CORE_DEPENDENCIES
        if (to !in allowed) {
            return "$from -> $to（feature 依赖的 core 模块超出白名单，仅允许依赖：$allowed）"
        }
    }
    // R9: feature:*:api 只允许依赖纯契约（navigation-contract），正向断言。
    // api 是「路由键契约」层，不得依赖运行时机制（navigation-runtime）、领域端口（domain）、
    // UI（ui/designsystem）等任何运行时/领域模块，防止契约层被污染后失去可复用性。
    if (from.startsWith(":feature:") && from.endsWith(":api") && to.startsWith(":core:")) {
        val allowed = FEATURE_API_ALLOWED_CORE_DEPENDENCIES
        if (to !in allowed) {
            return "$from -> $to（feature:*:api 只允许依赖纯契约，超出白名单，仅允许依赖：$allowed）"
        }
    }
    // 新增：core 模块内部依赖白名单（正向声明，未声明的 core→core 边一律失败）
    if (from.startsWith(":core:") && to.startsWith(":core:")) {
        val allowed = CORE_ALLOWED_DEPENDENCIES[from]
        if (allowed == null || to !in allowed) {
            return "$from -> $to（core 模块依赖超出白名单，仅允许依赖：$allowed）"
        }
    }
    return null
}

/**
 * 依赖环检测：对模块依赖图做 DFS，报告所有依赖环。
 * core 模块必须是无环 DAG，任何环都属架构违规。
 */
internal fun detectCycles(edges: Map<String, Set<String>>): List<String> {
    val cycles = mutableListOf<String>()
    val visiting = mutableSetOf<String>()
    val visited = mutableSetOf<String>()
    val stack = mutableListOf<String>()

    fun dfs(node: String) {
        if (node in visited || node in visiting) return
        visiting.add(node)
        stack.add(node)
        for (next in edges[node].orEmpty()) {
            if (next in visiting) {
                val start = stack.indexOf(next)
                val cycle = stack.subList(start, stack.size) + next
                cycles.add(cycle.joinToString(" -> "))
            } else if (next !in visited) {
                dfs(next)
            }
        }
        stack.removeAt(stack.size - 1)
        visiting.remove(node)
        visited.add(node)
    }

    for (node in edges.keys) {
        dfs(node)
    }
    return cycles.distinct()
}

/**
 * 匹配 `import androidx.core.<pkg>.X`，捕获其中的子包名。
 *
 * 例：`import androidx.core.net.toUri` → `net`；`import androidx.core.splashscreen.SplashScreen` → `splashscreen`。
 */
internal val CORE_IMPORT_REGEX = Regex("""import androidx\.core\.([A-Za-z0-9_]+)\.""")

/**
 * `androidx.core.*` 子包 → 可提供该子包的构件（命中其一即视为已显式声明）。
 *
 * 只有**不属于** core/core-ktx 的独立构件才需要列在这里；未列出的子包走 [CORE_DEFAULT_ARTIFACTS]。
 */
internal val CORE_ARTIFACT_BY_PACKAGE: Map<String, Set<String>> = mapOf(
    // androidx.core.splashscreen 是独立发布单元，core-ktx 不包含它
    "splashscreen" to setOf("core-splashscreen"),
)

/**
 * 其余 `androidx.core.*` 子包（net / view / content / graphics / os / app / widget / text ...）
 * 由 `core-ktx` 或 `core` 提供——core-ktx 依赖 core，因此声明任一即可。
 */
internal val CORE_DEFAULT_ARTIFACTS = setOf("core-ktx", "core")

/** 解析一行源码：若是 `androidx.core.<pkg>.` 的 import，返回子包名，否则返回 null。 */
internal fun parseCoreImportPackage(line: String): String? =
    CORE_IMPORT_REGEX.find(line.trim())?.groupValues?.get(1)

/** 某子包需要的构件集合：命中 [CORE_ARTIFACT_BY_PACKAGE] 则用之，否则回落到 [CORE_DEFAULT_ARTIFACTS]。 */
internal fun requiredCoreArtifacts(pkg: String): Set<String> =
    CORE_ARTIFACT_BY_PACKAGE[pkg] ?: CORE_DEFAULT_ARTIFACTS

/**
 * 找出源码用到、但声明中不包含任何所需构件的子包。
 *
 * 返回 (子包名, 所需构件集合) 列表，顺序与 [usedPackages] 的遍历顺序一致。
 */
internal fun unmetCoreImports(
    usedPackages: Set<String>,
    declared: Set<String>,
): List<Pair<String, Set<String>>> =
    usedPackages.mapNotNull { pkg ->
        val required = requiredCoreArtifacts(pkg)
        if (declared.none { it in required }) pkg to required else null
    }
