package com.example.fragmject.core.domain.usecase

import com.example.fragmject.core.domain.repository.MyRepository
import com.example.fragmject.core.domain.result.CollectResult
import javax.inject.Inject

/**
 * 收藏 / 取消收藏文章领域用例。
 *
 * 统一封装 [MyRepository] 的收藏与取消收藏两个入口，
 * 供各 ViewModel 注入后暴露给 UI 层，替代原先 Composable 内的
 * Hilt EntryPoint 服务定位（rememberCollectAction）。
 */
class CollectArticleUseCase @Inject constructor(
    private val repo: MyRepository,
) {
    suspend operator fun invoke(id: String, collect: Boolean): CollectResult =
        if (collect) repo.collectArticle(id) else repo.uncollectArticle(id)
}
