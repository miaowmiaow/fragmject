package com.example.fragmject.core.domain.usecase

import com.example.fragmject.core.data.repository.MyCollectRepository
import javax.inject.Inject

/**
 * 刷新我的收藏缓存。
 */
class RefreshMyCollectUseCase @Inject constructor(
    private val repo: MyCollectRepository,
) {
    suspend operator fun invoke(): Int? = repo.refreshMyCollect()
}