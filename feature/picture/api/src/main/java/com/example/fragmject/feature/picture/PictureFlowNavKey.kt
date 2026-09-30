package com.example.fragmject.feature.picture

import androidx.navigation3.runtime.NavKey

/**
 * 标记接口：图片选择/预览/编辑流程的路由 key。
 *
 * 供 app 组合根（AppNavGraph）识别「图片流程是否仍在 backStack 中」，
 * 从而驱动流程级 [PictureFlowState] 的创建与销毁，取代进程级 @Singleton 单例。
 */
interface PictureFlowNavKey : NavKey
