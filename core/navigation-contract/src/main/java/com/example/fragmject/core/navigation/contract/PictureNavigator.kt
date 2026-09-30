package com.example.fragmject.core.navigation.contract

import kotlinx.coroutines.flow.StateFlow

/**
 * 图片域导航契约（语义动作，不含任何 NavKey）。
 *
 * 由 `:app` 组合根实现并映射到具体路由；调用方（demo 等）依赖本接口
 * 而非 picture feature 的 NavKey。选图结果经 [selectedUris] 以 StateFlow
 * 暴露，发起方订阅即可，无需回调。
 */
interface PictureNavigator {

    /** 打开图片选择器。 */
    fun openPictureSelector()

    /** 打开图片预览。 */
    fun openPicturePreview(uris: List<String>)

    /** 打开图片编辑。 */
    fun openPictureEditor(oldUriString: String)

    /** 选择器确认后上报选中结果（由图片流程在「确定」时调用）。 */
    fun onPictureSelected(uris: List<String>)

    /** 最近一次选图结果（发起方订阅以更新 UI）。 */
    val selectedUris: StateFlow<List<String>>
}
