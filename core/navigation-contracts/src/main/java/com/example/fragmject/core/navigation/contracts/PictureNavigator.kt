package com.example.fragmject.core.navigation.contracts

/**
 * 图片域导航契约（语义动作，不含任何 NavKey）。
 *
 * 由 `:app` 组合根实现并映射到具体路由；调用方（demo 等）依赖本接口
 * 而非 picture feature 的 NavKey。
 */
interface PictureNavigator {

    /** 打开图片选择器。 */
    fun openPictureSelector()

    /** 打开图片预览。 */
    fun openPicturePreview(uris: List<String>)

    /** 打开图片编辑。 */
    fun openPictureEditor(oldUriString: String)
}
