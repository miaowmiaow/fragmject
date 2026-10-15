package com.example.fragmject.app.navigation

import android.content.Intent
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.deeplink.BackStackMatchResult
import androidx.navigation3.runtime.deeplink.DeepLinkMatcher
import androidx.navigation3.runtime.deeplink.DeepLinkRequest
import androidx.navigation3.runtime.deeplink.DeepLinkUri
import androidx.navigation3.runtime.deeplink.UriDeepLinkMatcher
import androidx.navigation3.runtime.deeplink.withBackStack
import com.example.fragmject.core.webview.WebViewCommons
import com.example.fragmject.feature.article.WebNavKey
import com.example.fragmject.feature.home.MainNavKey
import com.example.fragmject.feature.search.SearchNavKey
import com.example.fragmject.feature.user.RankNavKey

/**
 * 深层链接匹配器集合（基于 Navigation 3 的 DeepLinkRequest / DeepLinkMatcher API）。
 *
 * URI 模式与 AndroidManifest.xml 中 MainActivity 的 intent-filter 保持一致：
 * scheme = fragmject，host = com.example.fragment.project。
 *
 * 支持以下深层链接：
 * - fragmject://com.example.fragment.project/rank         → RankNavKey（积分排行榜）
 * - fragmject://com.example.fragment.project/search/{key} → SearchNavKey（搜索关键词）
 * - fragmject://com.example.fragment.project/web/{url}    → WebNavKey（WebView 文章详情，url 需编码）
 *
 * 每个匹配器通过 withBackStack 合成返回栈 [MainNavKey, 目标 Key]，
 * 保证从深层链接页面按返回键时先回到首页，而非直接退出应用（空栈）。
 * 调用示例代码如下：
 * val deepLinkIntent = Intent(
 *     Intent.ACTION_VIEW,
 *     "fragmject://com.example.fragment.project/web/${Uri.encode("http://www.baidu.com")}".toUri(),
 * )
 * val deepLinkPendingIntent: PendingIntent? = TaskStackBuilder.create(this).run {
 *     addNextIntentWithParentStack(deepLinkIntent)
 *     getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
 * }
 * deepLinkPendingIntent?.send()
 */
/**
 * **惰性构造**：`UriDeepLinkMatcher` 内部会 `android.net.Uri.parse`，若写成普通顶层 val，
 * 类初始化就会触发它——纯 JVM 单测（无 Robolectric）加载本文件时直接抛
 * `Method parse in android.net.Uri not mocked`，连只验证 [isAllowedDeepLinkTarget] 的用例都跑不起来。
 * 改为 `by lazy` 后，只有真正走深链匹配才构造，与「冷启动不一定有深链」的实际路径也一致。
 */
val deepLinkMatchers: List<DeepLinkMatcher<*, *>> by lazy {
    listOf(
        UriDeepLinkMatcher(
            uriPattern = DeepLinkUri("fragmject://com.example.fragment.project/rank"),
            serializer = RankNavKey.serializer(),
        ).withBackStack { matchResult -> listOf(MainNavKey, matchResult.key) },

        UriDeepLinkMatcher(
            uriPattern = DeepLinkUri("fragmject://com.example.fragment.project/search/{key}"),
            serializer = SearchNavKey.serializer(),
        ).withBackStack { matchResult -> listOf(MainNavKey, matchResult.key) },

        UriDeepLinkMatcher(
            uriPattern = DeepLinkUri("fragmject://com.example.fragment.project/web/{url}"),
            serializer = WebNavKey.serializer(),
        ).withBackStack { matchResult -> listOf(MainNavKey, matchResult.key) },
    )
}

/**
 * 将 [DeepLinkRequest] 匹配为目标 back stack；无匹配返回 null。
 */
fun matchDeepLink(request: DeepLinkRequest): List<NavKey>? {
    val matchResult = deepLinkMatchers.firstNotNullOfOrNull { it.match(request) }
        ?: return null
    val backStack = when (matchResult) {
        is BackStackMatchResult<*, *> -> {
            @Suppress("UNCHECKED_CAST")
            matchResult.backStack as List<NavKey>
        }

        else -> listOf(matchResult.key as NavKey)
    }
    // 目标参数不合法（如 web 深链携带 javascript:/content: 等协议）时整体丢弃该深链
    return backStack.takeIf { stack -> stack.all(::isAllowedDeepLinkTarget) }
}

/**
 * 深链目标合法性：外部可控的参数必须通过白名单。
 * web 深链只允许 http/https（assets 内置页仅限应用内部导航，不对外部深链开放）。
 */
internal fun isAllowedDeepLinkTarget(key: NavKey): Boolean = when (key) {
    is WebNavKey -> WebViewCommons.isHttpUrl(key.url)
    else -> true
}

/**
 * 将深层链接 Intent 解析为初始 back stack；无 data 或未匹配时回退到首页 [MainNavKey]。
 */
fun resolveDeepLinkBackStack(intent: Intent?): List<NavKey> {
    val uri = intent?.data ?: return listOf(MainNavKey)
    return matchDeepLink(DeepLinkRequest(uri = uri)) ?: listOf(MainNavKey)
}
