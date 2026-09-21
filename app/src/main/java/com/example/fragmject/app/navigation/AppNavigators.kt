package com.example.fragmject.app.navigation

import com.example.fragmject.core.navigation.contracts.ArticleNavigator
import com.example.fragmject.core.navigation.contracts.AuthNavigator
import com.example.fragmject.core.navigation.contracts.CollectionNavigator
import com.example.fragmject.core.navigation.contracts.DemoNavigator
import com.example.fragmject.core.navigation.contracts.HomeNavigator
import com.example.fragmject.core.navigation.contracts.PictureNavigator
import com.example.fragmject.core.navigation.contracts.SearchNavigator
import com.example.fragmject.core.navigation.contracts.UserNavigator
import com.example.fragmject.feature.article.WebNavKey
import com.example.fragmject.feature.auth.LoginNavKey
import com.example.fragmject.feature.auth.RegisterNavKey
import com.example.fragmject.feature.collection.MyCollectNavKey
import com.example.fragmject.feature.collection.MyShareNavKey
import com.example.fragmject.feature.collection.ShareArticleNavKey
import com.example.fragmject.feature.demo.DemoNavKey
import com.example.fragmject.feature.home.MainNavKey
import com.example.fragmject.feature.home.SystemNavKey
import com.example.fragmject.feature.picture.PictureEditorNavKey
import com.example.fragmject.feature.picture.PicturePreviewNavKey
import com.example.fragmject.feature.picture.PictureSelectorNavKey
import com.example.fragmject.feature.search.SearchNavKey
import com.example.fragmject.feature.user.BrowseHistoryNavKey
import com.example.fragmject.feature.user.MyCoinNavKey
import com.example.fragmject.feature.user.SettingNavKey
import com.example.fragmject.feature.user.UserNavKey
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 语义导航契约的 app 组合根实现。
 *
 * 这里是全项目唯一把「跨域语义动作」映射为「具体 feature NavKey」的位置。
 * 业务 Feature 只依赖 [com.example.fragmject.core.navigation.contracts] 下的接口，
 * 不感知任何目标 NavKey，从而消除横向路由耦合。
 */

@Singleton
class AppArticleNavigator @Inject constructor(
    private val dispatcher: NavigationDispatcher,
) : ArticleNavigator {
    override fun openArticle(url: String) = dispatcher.navigate(WebNavKey(url))
}

@Singleton
class AppUserNavigator @Inject constructor(
    private val dispatcher: NavigationDispatcher,
) : UserNavigator {
    override fun openUserProfile(userId: String) = dispatcher.navigate(UserNavKey(userId))
    override fun openSetting() = dispatcher.navigate(SettingNavKey)
    override fun openMyCoin() = dispatcher.navigate(MyCoinNavKey)
    override fun openBrowseHistory() = dispatcher.navigate(BrowseHistoryNavKey)
}

@Singleton
class AppAuthNavigator @Inject constructor(
    private val dispatcher: NavigationDispatcher,
) : AuthNavigator {
    override fun openLogin() = dispatcher.navigate(LoginNavKey)
    override fun openRegister() = dispatcher.navigate(RegisterNavKey)
}

@Singleton
class AppCollectionNavigator @Inject constructor(
    private val dispatcher: NavigationDispatcher,
) : CollectionNavigator {
    override fun openMyCollect() = dispatcher.navigate(MyCollectNavKey)
    override fun openMyShare() = dispatcher.navigate(MyShareNavKey)
    override fun openShareArticle() = dispatcher.navigate(ShareArticleNavKey)
}

@Singleton
class AppSearchNavigator @Inject constructor(
    private val dispatcher: NavigationDispatcher,
) : SearchNavigator {
    override fun openSearch(key: String) = dispatcher.navigate(SearchNavKey(key))
}

@Singleton
class AppDemoNavigator @Inject constructor(
    private val dispatcher: NavigationDispatcher,
) : DemoNavigator {
    override fun openDemo() = dispatcher.navigate(DemoNavKey)
}

@Singleton
class AppPictureNavigator @Inject constructor(
    private val dispatcher: NavigationDispatcher,
) : PictureNavigator {
    override fun openPictureSelector() = dispatcher.navigate(PictureSelectorNavKey)
    override fun openPicturePreview(uris: List<String>) = dispatcher.navigate(PicturePreviewNavKey(uris))
    override fun openPictureEditor(oldUriString: String) = dispatcher.navigate(PictureEditorNavKey(oldUriString))
}

@Singleton
class AppHomeNavigator @Inject constructor(
    private val dispatcher: NavigationDispatcher,
) : HomeNavigator {
    override fun openMain() = dispatcher.popBackStack(MainNavKey)
    override fun openSystem(cid: String) = dispatcher.navigate(SystemNavKey(cid))
}
