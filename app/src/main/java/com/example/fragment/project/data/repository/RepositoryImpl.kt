package com.example.fragment.project.data.repository

import com.example.fragment.project.data.ArticleList
import com.example.fragment.project.data.BannerList
import com.example.fragment.project.data.CoinRank
import com.example.fragment.project.data.HotKeyList
import com.example.fragment.project.data.Login
import com.example.fragment.project.data.MyCoinList
import com.example.fragment.project.data.NavigationList
import com.example.fragment.project.data.ProjectTreeList
import com.example.fragment.project.data.Register
import com.example.fragment.project.data.ShareArticleList
import com.example.fragment.project.data.TopArticle
import com.example.fragment.project.data.TreeList
import com.example.fragment.project.data.UserCoin
import com.example.miaow.base.http.CoroutineHttp
import com.example.miaow.base.http.HttpRequest
import com.example.miaow.base.http.HttpResponse

/**
 * 顶层 helper：把 [CoroutineHttp] 实例方法包装成不依赖 [kotlinx.coroutines.CoroutineScope] 接收者的形式。
 *
 * 之所以不直接复用 `com.example.miaow.base.http.get/post`：那两个函数是 `CoroutineScope` 的扩展，
 * 但 Repository 的 suspend 方法语义上不需要也不应该耦合 `CoroutineScope` —— suspend 本身已经携带了
 * 协程上下文。这里走 [CoroutineHttp.get] / [CoroutineHttp.post] 的纯实例方法，类型由 reified 推断。
 */
private suspend inline fun <reified T : HttpResponse> httpGet(
    noinline init: HttpRequest.() -> Unit,
): T = CoroutineHttp.getInstance().get(init, T::class.java)

private suspend inline fun <reified T : HttpResponse> httpPost(
    noinline init: HttpRequest.() -> Unit,
): T = CoroutineHttp.getInstance().post(init, T::class.java)

/**
 * 网络版本的 [ArticleRepository] 实现。
 *
 * 这里是 ViewModel 与底层 HTTP 工具之间唯一的胶水层；
 * 所有 URL、Path、Query、Param 拼装都收敛在此文件，便于：
 *   1. 接口路径变更时只改一处；
 *   2. 后续接入 Mock 或缓存策略时无需改 ViewModel；
 *   3. SWR 缓存版（xxxFlow）与 suspend 版共用同一份 DSL（spec），
 *      参数变更不会出现"改了请求但忘改 cacheKey"的问题。
 */
internal class ArticleRepositoryImpl : ArticleRepository {

    // ===== 请求 spec：suspend 版与 Flow 版共用，URL/参数变更只改这里 =====

    private fun bannerSpec(): HttpRequest.() -> Unit = {
        setUrl("banner/json")
    }

    private fun articleTopSpec(): HttpRequest.() -> Unit = {
        setUrl("article/top/json")
    }

    private fun articleListSpec(page: Int): HttpRequest.() -> Unit = {
        setUrl("article/list/{page}/json")
        putPath("page", page.toString())
    }

    private fun articleListByCidSpec(cid: String, page: Int): HttpRequest.() -> Unit = {
        setUrl("article/list/{page}/json")
        putPath("page", page.toString())
        putQuery("cid", cid)
    }

    // ===== suspend 版 =====

    override suspend fun getArticleList(page: Int): ArticleList = httpGet(articleListSpec(page))

    override suspend fun getArticleListByCid(cid: String, page: Int): ArticleList =
        httpGet(articleListByCidSpec(cid, page))

    override suspend fun searchArticles(key: String, page: Int): ArticleList = httpPost {
        setUrl("article/query/{page}/json")
        putParam("k", key)
        putPath("page", page.toString())
    }

    override suspend fun getCollectList(page: Int): ArticleList = httpGet {
        setUrl("lg/collect/list/{page}/json")
        putPath("page", page.toString())
    }

    // ===== Flow 版（带 SWR 缓存） =====

    override fun getBannerFlow() = cachedHttpGet<BannerList>(bannerSpec())

    override fun getArticleTopFlow() = cachedHttpGet<TopArticle>(articleTopSpec())

    override fun getArticleListFlow(page: Int) =
        cachedHttpGet<ArticleList>(articleListSpec(page))

    override fun getArticleListByCidFlow(cid: String, page: Int) =
        cachedHttpGet<ArticleList>(articleListByCidSpec(cid, page))
}

internal class ProjectRepositoryImpl : ProjectRepository {

    private fun projectListSpec(cid: String, page: Int): HttpRequest.() -> Unit = {
        setUrl("project/list/{page}/json")
        putPath("page", page.toString())
        putQuery("cid", cid)
    }

    private fun projectTreeSpec(): HttpRequest.() -> Unit = {
        setUrl("project/tree/json")
    }

    override suspend fun getProjectList(cid: String, page: Int): ArticleList =
        httpGet(projectListSpec(cid, page))

    override fun getProjectListFlow(cid: String, page: Int) =
        cachedHttpGet<ArticleList>(projectListSpec(cid, page))

    override fun getProjectTreeFlow() = cachedHttpGet<ProjectTreeList>(projectTreeSpec())
}

internal class UserRepositoryImpl : UserRepository {

    override suspend fun login(username: String, password: String): Login = httpPost {
        setUrl("user/login")
        putParam("username", username)
        putParam("password", password)
    }

    override suspend fun register(
        username: String,
        password: String,
        repassword: String,
    ): Register = httpPost {
        setUrl("user/register")
        putParam("username", username)
        putParam("password", password)
        putParam("repassword", repassword)
    }

    override suspend fun logout(): HttpResponse = httpGet {
        setUrl("user/logout/json")
    }

    override suspend fun getUserShareArticles(userId: String, page: Int): ShareArticleList = httpGet {
        setUrl("user/{id}/share_articles/{page}/json")
        putPath("id", userId)
        putPath("page", page.toString())
    }
}

internal class MyRepositoryImpl : MyRepository {

    override suspend fun getUserCoin(): UserCoin = httpGet {
        setUrl("lg/coin/userinfo/json")
    }

    override suspend fun getMyCoinList(page: Int): MyCoinList = httpGet {
        setUrl("lg/coin/list/{page}/json")
        putPath("page", page.toString())
    }

    override suspend fun getMyShareList(page: Int): ShareArticleList = httpGet {
        setUrl("user/lg/private_articles/{page}/json")
        putPath("page", page.toString())
    }

    override suspend fun shareArticle(title: String, link: String): HttpResponse = httpPost {
        setUrl("lg/user_article/add/json")
        putParam("title", title)
        putParam("link", link)
    }

    override suspend fun collectArticle(id: String): HttpResponse = httpPost {
        setUrl("lg/collect/{id}/json")
        putPath("id", id)
    }

    override suspend fun uncollectArticle(id: String): HttpResponse = httpPost {
        setUrl("lg/uncollect_originId/{id}/json")
        putPath("id", id)
    }
}

internal class CommonRepositoryImpl : CommonRepository {

    private fun navigationSpec(): HttpRequest.() -> Unit = {
        setUrl("navi/json")
    }

    private fun systemTreeSpec(): HttpRequest.() -> Unit = {
        setUrl("tree/json")
    }

    private fun hotKeySpec(): HttpRequest.() -> Unit = {
        setUrl("hotkey/json")
    }

    private fun coinRankSpec(page: Int): HttpRequest.() -> Unit = {
        setUrl("coin/rank/{page}/json")
        putPath("page", page.toString())
    }

    override suspend fun getCoinRank(page: Int): CoinRank = httpGet(coinRankSpec(page))

    override fun getNavigationFlow() = cachedHttpGet<NavigationList>(navigationSpec())

    override fun getSystemTreeFlow() = cachedHttpGet<TreeList>(systemTreeSpec())

    override fun getHotKeyFlow() = cachedHttpGet<HotKeyList>(hotKeySpec())

    override fun getCoinRankFlow(page: Int) = cachedHttpGet<CoinRank>(coinRankSpec(page))
}