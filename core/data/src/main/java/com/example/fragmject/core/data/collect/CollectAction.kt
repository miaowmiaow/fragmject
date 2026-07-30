package com.example.fragmject.core.data.collect

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import dagger.hilt.android.EntryPointAccessors

/**
 * Composable 中获取收藏操作的便捷封装。
 *
 * 通过 Hilt EntryPoint 获取 [MyRepository]，返回 suspend 闭包：
 * `(articleId: String, collect: Boolean) -> Unit`
 *
 * 调用方（如 ArticleCard）只需在 Compose 函数体内调用此方法获取 action，
 * 然后在协程中调用返回值即可完成收藏/取消收藏。
 *
 * @return suspend 闭包：articleId + 是否收藏
 */
@Composable
fun rememberCollectAction(): suspend (String, Boolean) -> Unit {
    val context = LocalContext.current
    val myRepo = remember {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            WanEntryPoint::class.java,
        ).myRepo
    }
    return { id, collect ->
        val response = if (collect) myRepo.collectArticle(id)
        else myRepo.uncollectArticle(id)
        if (response.errorCode != "0") {
            // 失败由调用方自行处理（如跳转登录）
        }
    }
}
