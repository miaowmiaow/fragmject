package com.example.fragmject.feature.picture.nav

import androidx.navigation3.runtime.NavKey
import com.example.fragmject.core.domain.repository.AlbumRepository
import com.example.fragmject.core.domain.repository.MediaRepository
import com.example.fragmject.core.navigation.runtime.NavFlowScope
import com.example.fragmject.core.navigation.runtime.NavFlowScopeContributor
import com.example.fragmject.feature.picture.PictureFlowNavKey
import com.example.fragmject.feature.picture.state.PictureFlowState
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 图片流程作用域贡献者。
 *
 * 实现 navigation-runtime 的通用 [NavFlowScopeContributor]：声明某 NavKey 是否属于
 * 图片流程（[PictureFlowNavKey]），并为每次图片流程创建独立的 [PictureFlowState]。
 * app 组合根仅依赖通用接口，不感知 PictureFlowState / Album / MediaItem 等类型。
 */
@Singleton
class PictureFlowScopeContributor @Inject constructor(
    private val mediaRepository: MediaRepository,
    private val albumRepository: AlbumRepository,
) : NavFlowScopeContributor {

    override fun matches(key: NavKey): Boolean = key is PictureFlowNavKey

    override fun create(): NavFlowScope = PictureFlowState(mediaRepository, albumRepository)
}
