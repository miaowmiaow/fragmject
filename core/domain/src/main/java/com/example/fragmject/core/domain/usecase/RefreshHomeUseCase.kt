package com.example.fragmject.core.domain.usecase

import com.example.fragmject.core.domain.repository.HomeRepository
import com.example.fragmject.core.domain.result.DomainResult
import javax.inject.Inject

/**
 * 刷新首页数据：触发网络拉取并写 Room 缓存。
 *
 * @return 领域结果：成功携带总页数，失败携带错误码与消息。
 */
class RefreshHomeUseCase @Inject constructor(
    private val homeRepo: HomeRepository,
) {
    suspend operator fun invoke(): DomainResult<Int> = homeRepo.refreshHome()
}