package com.example.fragmject.core.domain.usecase

import com.example.fragmject.core.data.repository.HomeRepository
import javax.inject.Inject

/**
 * 刷新首页数据：触发网络拉取并写 Room 缓存。
 *
 * @return 总页数，用于分页判断；失败时返回 null。
 */
class RefreshHomeUseCase @Inject constructor(
    private val homeRepo: HomeRepository,
) {
    suspend operator fun invoke(): Int? = homeRepo.refreshHome()
}