package com.example.fragmject.core.data

import com.example.fragmject.core.database.dao.ArticleDao
import com.example.fragmject.core.database.model.ArticleEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * 内存版 [ArticleDao]：以 Map + [MutableStateFlow] 模拟 Room 的单表缓存与 Flow 发射。
 *
 * 供 core:data 的 Repository 测试复用，替代真实的 Room in-memory 数据库，
 * 使纯 JVM 单测无需 Android Context 即可覆盖 offline-first 写库逻辑。
 */
class FakeArticleDao : ArticleDao {

    private val store = LinkedHashMap<String, MutableList<ArticleEntity>>()
    private val flows = HashMap<String, MutableStateFlow<List<ArticleEntity>>>()

    private fun flowFor(key: String): MutableStateFlow<List<ArticleEntity>> =
        flows.getOrPut(key) { MutableStateFlow(emptyList()) }

    private fun refreshFlows() {
        store.forEach { (key, list) -> flowFor(key).value = list.toList() }
    }

    fun count(key: String): Int = store[key]?.size ?: 0

    fun snapshot(key: String): List<ArticleEntity> = store[key]?.toList() ?: emptyList()

    override suspend fun insertAll(articles: List<ArticleEntity>) {
        articles.forEach { article ->
            val list = store.getOrPut(article.cacheKey) { ArrayList() }
            list.removeAll { it.articleId == article.articleId }
            list.add(article)
            list.sortBy { it.sortOrder }
        }
        refreshFlows()
    }

    override suspend fun replaceAll(cacheKey: String, entities: List<ArticleEntity>) {
        deleteByCacheKey(cacheKey)
        insertAll(entities)
    }

    override fun getByCacheKey(cacheKey: String): Flow<List<ArticleEntity>> = flowFor(cacheKey)

    override suspend fun deleteByCacheKey(cacheKey: String) {
        store.remove(cacheKey)
        flowFor(cacheKey).value = emptyList()
    }

    override suspend fun deleteByPrefix(prefix: String) {
        store.keys.filter { it.startsWith(prefix) }.toList().forEach { deleteByCacheKey(it) }
    }

    override suspend fun cleanExpired(threshold: Long) {
        store.replaceAll { _, list ->
            list.filter { it.timestamp >= threshold }.toMutableList()
        }
        refreshFlows()
    }

    override suspend fun countByCacheKey(cacheKey: String): Int = count(cacheKey)

    override fun getByPagePrefix(prefix: String): Flow<List<ArticleEntity>> = flowFor(prefix)
}
