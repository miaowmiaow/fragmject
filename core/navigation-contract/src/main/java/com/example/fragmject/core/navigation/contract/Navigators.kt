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
 * **新增一个语义动作要改三处**：`XxxNavigator` 接口 → 本聚合体的字段 → [Navigators.noop]。
 * 契约方法没有默认实现，前两处漏改会直接编译失败（app 侧的 `AppXxxNavigator` 同理），
 * 以此杜绝「新增了动作却没人实现、点击静默无效」。
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
    val authenticator: AuthenticatorNavigator,
) {
    companion object {
        /**
         * 全部空实现的 NoOp 兜底，供 Preview / 未提供实现时使用。
         *
         * **为什么要把每个方法都显式写出来**：契约接口**故意不给默认实现**
         * （没有 `= Unit`），新增一个语义动作时这里与 `AppNavigators` 都会编译失败，
         * 强制同步；若接口带默认实现，漏实现会静默变成「点了没反应」。
         */
        fun noop(): Navigators = Navigators(
            article = object : ArticleNavigator {
                override fun openArticle(url: String) = Unit
            },
            user = object : UserNavigator {
                override fun openUserProfile(userId: String) = Unit
                override fun openSetting() = Unit
                override fun openMyCoin() = Unit
                override fun openBrowseHistory() = Unit
                override fun openRank() = Unit
            },
            auth = object : AuthNavigator {
                override fun openLogin() = Unit
                override fun openRegister() = Unit
                override fun onAuthSuccess() = Unit
            },
            collection = object : CollectionNavigator {
                override fun openMyCollect() = Unit
                override fun openMyShare() = Unit
                override fun openShareArticle() = Unit
            },
            search = object : SearchNavigator {
                override fun openSearch(key: String) = Unit
            },
            demo = object : DemoNavigator {
                override fun openDemo() = Unit
            },
            picture = object : PictureNavigator {
                override val selectedUris: StateFlow<List<String>> = MutableStateFlow(emptyList())
                override fun openPictureSelector() = Unit
                override fun openPicturePreview(uris: List<String>) = Unit
                override fun openPicturePreviewForSelection(uris: List<String>) = Unit
                override fun openPictureEditor(uriString: String) = Unit
                override fun onPictureSelected(uris: List<String>) = Unit
                override fun clearSelection() = Unit
            },
            home = object : HomeNavigator {
                override fun openMain() = Unit
                override fun openSystemTree(cid: String) = Unit
            },
            authenticator = object : AuthenticatorNavigator {
                override fun openAuthenticator() = Unit
                override fun openAuthenticatorAdd() = Unit
                override fun openAuthenticatorTransfer() = Unit
                override fun openAuthenticatorExportQr(ids: List<Long>, mode: AuthenticatorExportMode) = Unit
            },
        )
    }
}

val LocalNavigators = staticCompositionLocalOf { Navigators.noop() }