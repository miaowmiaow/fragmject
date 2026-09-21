package com.example.fragmject.core.navigation.contracts

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * 语义导航契约的 Compose 访问入口。
 *
 * 跨域导航是 UI 层职责，通过 CompositionLocal 在 Composable 中直接取用，
 * 避免将 Navigator 注入 ViewModel（违反 UI 层分层）。默认值指向 NoOp 空实现，
 * 使 Preview 或未由 app 组合根提供实现时仍能安全调用。
 *
 * 使用 [androidx.compose.runtime.staticCompositionLocalOf]：Navigator 为稳定单例、
 * 内容不变，无需触发订阅方重组。
 */

val LocalArticleNavigator = staticCompositionLocalOf<ArticleNavigator> { NoOpArticleNavigator }
val LocalUserNavigator = staticCompositionLocalOf<UserNavigator> { NoOpUserNavigator }
val LocalAuthNavigator = staticCompositionLocalOf<AuthNavigator> { NoOpAuthNavigator }
val LocalCollectionNavigator = staticCompositionLocalOf<CollectionNavigator> { NoOpCollectionNavigator }
val LocalSearchNavigator = staticCompositionLocalOf<SearchNavigator> { NoOpSearchNavigator }
val LocalDemoNavigator = staticCompositionLocalOf<DemoNavigator> { NoOpDemoNavigator }
val LocalPictureNavigator = staticCompositionLocalOf<PictureNavigator> { NoOpPictureNavigator }
val LocalHomeNavigator = staticCompositionLocalOf<HomeNavigator> { NoOpHomeNavigator }

private object NoOpArticleNavigator : ArticleNavigator {
    override fun openArticle(url: String) = Unit
}

private object NoOpUserNavigator : UserNavigator {
    override fun openUserProfile(userId: String) = Unit
    override fun openSetting() = Unit
    override fun openMyCoin() = Unit
    override fun openBrowseHistory() = Unit
}

private object NoOpAuthNavigator : AuthNavigator {
    override fun openLogin() = Unit
    override fun openRegister() = Unit
}

private object NoOpCollectionNavigator : CollectionNavigator {
    override fun openMyCollect() = Unit
    override fun openMyShare() = Unit
    override fun openShareArticle() = Unit
}

private object NoOpSearchNavigator : SearchNavigator {
    override fun openSearch(key: String) = Unit
}

private object NoOpDemoNavigator : DemoNavigator {
    override fun openDemo() = Unit
}

private object NoOpPictureNavigator : PictureNavigator {
    override fun openPictureSelector() = Unit
    override fun openPicturePreview(uris: List<String>) = Unit
    override fun openPictureEditor(oldUriString: String) = Unit
}

private object NoOpHomeNavigator : HomeNavigator {
    override fun openMain() = Unit
    override fun openSystem(cid: String) = Unit
}
