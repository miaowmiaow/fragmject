package com.example.fragmject.core.navigation.runtime

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavKey
import kotlin.reflect.KClass

/**
 * 统一导航内容注册表：以 NavKey 类型为 key，存储「NavKey → 渲染器」映射。
 *
 * 与 Navigation 3 的 [androidx.navigation3.runtime.EntryProviderScope] 解耦，
 * 使全屏 entry 与面板 detailContent 共用同一份渲染知识，消除重复维护的 when 分支。
 *
 * 渲染器签名 `(NavKey) -> Unit`；「返回上一级」语义由 [LocalOnNavigateUp]
 * CompositionLocal 提供，全屏 entry 与面板 detailContent 两条路径注入不同的返回实现。
 */
class NavContentRegistry {

    @PublishedApi
    internal val contents =
        LinkedHashMap<KClass<out NavKey>, @Composable (NavKey) -> Unit>()

    /**
     * 注册某类 NavKey 的渲染器。
     *
     * 由于 [KClass] 无法在运行时保留具体泛型，内部通过 [Suppress] 收窄强转，
     * 该强转由 `reified` 的类型参数保证安全（与 Navigation 3 内部实现同款处理）。
     */
    inline fun <reified K : NavKey> register(
        noinline content: @Composable (K) -> Unit,
    ) {
        check(K::class !in contents) {
            "Duplicate NavKey registration: ${K::class.simpleName}"
        }
        contents[K::class] = { key ->
            @Suppress("UNCHECKED_CAST")
            content(key as K)
        }
    }

    /**
     * 按 NavKey 类型渲染对应内容。
     *
     * 未注册即抛 [IllegalStateException]：遗漏注册属开发期编程错误，
     * 宁可尽早崩溃暴露，也不静默空白。
     */
    @Composable
    fun Render(key: NavKey) {
        val content = contents[key::class]
        check(content != null) {
            "No renderer registered for NavKey: ${key::class.simpleName}. " +
                "Ensure a NavContentContributor registers it."
        }
        content(key)
    }

    /** 遍历全部注册项，供 entryProvider 驱动全屏 entry 构建。 */
    fun forEach(
        action: (KClass<out NavKey>, @Composable (NavKey) -> Unit) -> Unit,
    ) {
        contents.forEach { (clazz, content) -> action(clazz, content) }
    }
}
