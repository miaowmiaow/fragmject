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

    /**
     * 清空选中结果。
     *
     * 由组合根在「**进入**图片流程」时调用（而非退出时）。
     *
     * 为什么是进入时：系统返回键/手势返回不会走选择器的取消回调，确实需要一个兜底清理；
     * 但若在流程退出时清理，会把 [onPictureSelected] 刚写入的确认结果一并擦掉
     * （表现为「选完图点确定，回到发起方却不显示」）。改为进入时清空后：
     * - 确认返回 → 结果是本次新写入的，正常显示；
     * - 系统返回 → 进入时已清空，发起方看到空结果，不会残留上一次的选中；
     * - 再次进入 → 清空，不会与上一次结果混淆。
     */
    fun clearSelection()

    /** 最近一次选图结果（发起方订阅以更新 UI）。 */
    val selectedUris: StateFlow<List<String>>
}
