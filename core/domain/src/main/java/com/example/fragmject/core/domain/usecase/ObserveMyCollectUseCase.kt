package com.example.fragmject.core.domain.usecase

import com.example.fragmject.core.domain.repository.MyCollectRepository
import com.example.fragmject.core.model.Article
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/**
 * 观察我的收藏文章（Room 唯一数据源）。
 */
class ObserveMyCollectUseCase @Inject constructor(
    private val myCollectRepo: MyCollectRepository,
) {
    operator fun invoke(): Flow<List<Article>> = myCollectRepo.observeMyCollect()
}
