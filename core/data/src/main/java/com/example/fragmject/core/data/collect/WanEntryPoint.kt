package com.example.fragmject.core.data.collect

import com.example.fragmject.core.data.repository.MyRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Hilt EntryPoint：供 Compose 组件（非 ViewModel）在无 @Inject 场景下获取单例 Repository。
 *
 * @see com.example.fragment.project.components.ArticleCard 使用本 EntryPoint 获取 [MyRepository]
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface WanEntryPoint {
    val myRepo: MyRepository
}
