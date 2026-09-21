package com.example.fragmject.feature.picture.di

import com.example.fragmject.feature.picture.ui.selector.PictureViewModel
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * 供 picture 模块内部 Composable 获取单例 [PictureViewModel] 的 Hilt EntryPoint。
 *
 * PictureViewModel 已改为 @Singleton，跨 Selector/Preview/Editor 三个导航 entry
 * 共享同一实例；通过 EntryPoint 获取，使 app 层无需 import PictureViewModel。
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface PictureViewModelEntryPoint {
    fun pictureViewModel(): PictureViewModel
}
