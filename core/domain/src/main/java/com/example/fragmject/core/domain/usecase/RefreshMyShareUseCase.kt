package com.example.fragmject.core.domain.usecase

import com.example.fragmject.core.domain.repository.MyRepository
import com.example.fragmject.core.domain.result.MyShareResult
import javax.inject.Inject

/**
 * 加载我分享的文章列表（刷新用，page 从 0 开始）。
 */
class RefreshMyShareUseCase @Inject constructor(
    private val repo: MyRepository,
) {
    suspend operator fun invoke(page: Int): MyShareResult {
        return repo.getMyShareList(page)
    }
}