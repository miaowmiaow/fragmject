package com.example.fragmject.core.navigation

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavKey
import kotlin.reflect.KClass

/**
 * 统一导航内容注册表：以 NavKey 类型为 key，存储「NavKey → 渲染器」映射。
 *
 * 与 Navigation 3 的 [androidx.navigation3.runtime.EntryProviderScope] 解耦，
 * 使全屏 entry 与面板 detailContent 共用同一份渲染知识，消除重复维护的 when 分支。
 *
 * 渲染器签名 `(NavKey, NavCallbacks) -> Unit` 将回调作为运行时参数传入，
 * 而非在注册时闭包捕获，从而支持两条路径注入不同的返回语义。
 */
class NavContentRegistry {

    @PublishedApi
    internal val contents =
        LinkedHashMap<KClass<out NavKey>, @Composable (NavKey, NavCallbacks) -> Unit>()

    /**
     * 注册某类 NavKey 的渲染器。
     *
     * 由于 [KClass] 无法在运行时保留具体泛型，内部通过 [Suppress] 收窄强转，
     * 该强转由 `reified` 的类型参数保证安全（与 Navigation 3 内部实现同款处理）。
     */
    inline fun <reified K : NavKey> register(
        noinline content: @Composable (K, NavCallbacks) -> Unit,
    ) {
        contents[K::class] = { key, callbacks ->
            @Suppress("UNCHECKED_CAST")
            content(key as K, callbacks)
        }
    }

    /** 按 NavKey 类型渲染对应内容；未注册则空渲染。 */
    @Composable
    fun Render(key: NavKey, callbacks: NavCallbacks) {
        contents[key::class]?.invoke(key, callbacks)
    }

    /** 遍历全部注册项，供 entryProvider 驱动全屏 entry 构建。 */
    fun forEach(
        action: (KClass<out NavKey>, @Composable (NavKey, NavCallbacks) -> Unit) -> Unit,
    ) {
        contents.forEach { (clazz, content) -> action(clazz, content) }
    }
}
