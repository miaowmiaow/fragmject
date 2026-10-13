package com.example.fragmject.core.navigation.contract

import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
 * 语义导航契约的聚合容器与 Compose 访问入口。
 *
 * [Navigators] 聚合全部语义 Navigator，[LocalNavigators] 是其在 Compose 中的唯一
 * 访问入口。跨域导航是 UI 层职责，通过 CompositionLocal 在 Composable 中直接取用，
 * 避免将 Navigator 注入 ViewModel（违反 UI 层分层）。默认值 [Navigators.noop] 指向
 * 空实现，使 Preview 或未由 app 组合根提供实现时仍能安全调用。
 *
 * 使用 [androidx.compose.runtime.staticCompositionLocalOf]：Navigators 为稳定单例、
 * 内容不变，无需触发订阅方重组。
 *
 * [Inject] 使 Hilt 能直接构造本聚合体，取代 app 组合根单独的 AppNavigatorBundle
 * Holder，减少样板。
 */
data class Navigators @Inject constructor(
    val article: ArticleNavigator,
    val user: UserNavigator,
    val auth: AuthNavigator,
    val collection: CollectionNavigator,
    val search: SearchNavigator,
    val demo: DemoNavigator,
    val picture: PictureNavigator,
    val home: HomeNavigator,
) {
    companion object {
        /** 全部空实现的 NoOp 兜底，供 Preview / 未提供实现时使用。 */
        fun noop(): Navigators = Navigators(
            article = object : ArticleNavigator {},
            user = object : UserNavigator {},
            auth = object : AuthNavigator {},
            collection = object : CollectionNavigator {},
            search = object : SearchNavigator {},
            demo = object : DemoNavigator {},
            picture = object : PictureNavigator {
                override val selectedUris: StateFlow<List<String>> = MutableStateFlow(emptyList())
            },
            home = object : HomeNavigator {},
        )
    }
}

val LocalNavigators = staticCompositionLocalOf<Navigators> { Navigators.noop() }