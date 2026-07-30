package com.example.fragmject.core.data.repository

import com.example.fragmject.core.data.runHttpResult
import com.example.fragmject.core.model.Article
import com.example.fragmject.core.model.Banner
import kotlinx.coroutines.flow.Flow

class HomeRepositoryImpl(
    private val offlineFirst: OfflineFirstArticleRepository,
) : HomeRepository {

    override fun observeHomeArticles(): Flow<List<Article>> =
        offlineFirst.observeHomeArticles()

    override fun observeHomeBanners(): Flow<List<Banner>> =
        offlineFirst.observeHomeBanners()

    override suspend fun refreshHome() = offlineFirst.refreshHome()

    override suspend fun loadNextPage(page: Int) = offlineFirst.loadNextPage(page)
}

class SearchRepositoryImpl(
    private val articleRepository: ArticleRepository,
) : SearchRepository {

    override suspend fun searchArticles(key: String, page: Int) =
        runHttpResult { articleRepository.searchArticles(key, page) }
}

class UserCenterRepositoryImpl(
    private val userRepository: UserRepository,
) : UserCenterRepository {

    override suspend fun getUserShareArticles(userId: String, page: Int) =
        runHttpResult { userRepository.getUserShareArticles(userId, page) }
}

class SystemRepositoryImpl(
    private val offlineFirst: OfflineFirstSystemRepository,
) : SystemRepository {

    override fun observeSystemArticles(cid: String): Flow<List<Article>> =
        offlineFirst.observeSystemArticles(cid)

    override suspend fun refreshSystemArticles(cid: String) =
        offlineFirst.refreshSystemArticles(cid)

    override suspend fun loadSystemNextPage(cid: String, page: Int) =
        offlineFirst.loadSystemNextPage(cid, page)
}

class MyCollectRepositoryImpl(
    private val offlineFirst: OfflineFirstMyCollectRepository,
) : MyCollectRepository {

    override fun observeMyCollect(): Flow<List<Article>> =
        offlineFirst.observeMyCollect()

    override suspend fun refreshMyCollect() =
        offlineFirst.refreshMyCollect()

    override suspend fun loadMyCollectNextPage(page: Int) =
        offlineFirst.loadMyCollectNextPage(page)
}