package com.example.fragmject.app.navigation

import android.content.Intent
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.deeplink.BackStackMatchResult
import androidx.navigation3.runtime.deeplink.DeepLinkMatcher
import androidx.navigation3.runtime.deeplink.DeepLinkRequest
import androidx.navigation3.runtime.deeplink.DeepLinkUri
import androidx.navigation3.runtime.deeplink.UriDeepLinkMatcher
import androidx.navigation3.runtime.deeplink.withBackStack
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
val deepLinkMatchers: List<DeepLinkMatcher<*, *>> = listOf(
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

/**
 * 将 [DeepLinkRequest] 匹配为目标 back stack；无匹配返回 null。
 */
fun matchDeepLink(request: DeepLinkRequest): List<NavKey>? {
    val matchResult = deepLinkMatchers.firstNotNullOfOrNull { it.match(request) }
        ?: return null
    return when (matchResult) {
        is BackStackMatchResult<*, *> -> {
            @Suppress("UNCHECKED_CAST")
            matchResult.backStack as List<NavKey>
        }

        else -> listOf(matchResult.key as NavKey)
    }
}

/**
 * 将深层链接 Intent 解析为初始 back stack；无 data 或未匹配时回退到首页 [MainNavKey]。
 */
fun resolveDeepLinkBackStack(intent: Intent?): List<NavKey> {
    val uri = intent?.data ?: return listOf(MainNavKey)
    return matchDeepLink(DeepLinkRequest(uri = uri)) ?: listOf(MainNavKey)
}
