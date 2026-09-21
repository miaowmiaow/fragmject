package com.example.fragmject.core.domain.usecase

import com.example.fragmject.core.domain.repository.NavigationRepository
import com.example.fragmject.core.domain.repository.SearchRepository
import com.example.fragmject.core.domain.result.DomainResult
import com.example.fragmject.core.model.HotKey
import com.example.fragmject.core.model.Tree
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * 主界面 Header 聚合用例。
 *
 * 将「热搜词（SearchRepository）+ 体系树（NavigationRepository）」两个领域端口的
 * 观察与刷新编排从 [MainViewModel] 下沉到领域层，ViewModel 不再直接编排多 Repository。
 *
 * - [hotKeys] / [trees] 暴露 Room 唯一数据源的观察流；
 * - [refreshAll] 并发刷新两个源，聚合「首个失败」为整体结果（任一失败不抛出，
 *   避免破坏调用方 loading 收敛逻辑）。
 */
class MainHeaderAggregateUseCase @Inject constructor(
    private val navigationRepository: NavigationRepository,
    private val searchRepository: SearchRepository,
) {

    val hotKeys: Flow<List<HotKey>> = searchRepository.observeHotKey()

    val trees: Flow<List<Tree>> = navigationRepository.observeSystemTree()

    /** 并发刷新热搜 + 体系树；任一失败返回该失败，否则成功。 */
    suspend fun refreshAll(): DomainResult<Unit> = coroutineScope {
        val treeDeferred = async { navigationRepository.refreshSystemTree() }
        val hotKeyDeferred = async { searchRepository.refreshHotKey() }
        val treeResult = treeDeferred.await()
        val hotKeyResult = hotKeyDeferred.await()
        when {
            treeResult is DomainResult.Failure -> treeResult
            hotKeyResult is DomainResult.Failure -> hotKeyResult
            else -> DomainResult.Success(Unit)
        }
    }
}
