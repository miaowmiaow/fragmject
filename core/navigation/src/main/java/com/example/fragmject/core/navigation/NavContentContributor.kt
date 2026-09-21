package com.example.fragmject.core.navigation

/**
 * Feature 导航内容贡献者。
 *
 * 每个 `feature:*:impl` 通过 Hilt `@IntoSet` multibinding 提供一个实现，
 * 在 [contribute] 中把自身「NavKey → 渲染器」映射注册进 [NavContentRegistry]。
 *
 * app 组合根遍历全部贡献者构造统一注册表，从而不再显式 import 各 feature 的
 * `registerXxxNavContents()`，实现导航注册的自主化与装配解耦。
 *
 * 实现类通常无构造依赖（仅调用 `registry.register<XxxNavKey> { ... }`），
 * 故以 `object` 或无参 `class` 提供均可。
 */
fun interface NavContentContributor {
    fun contribute(registry: NavContentRegistry)
}
