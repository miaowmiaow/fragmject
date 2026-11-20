package com.example.fragmject.core.domain.repository

/**
 * 图片选择流程的「当前选中项」持久化端口。
 *
 * 选中态本身是 UI 状态，但它**跨进程重建必须可恢复**：选择器/预览页的返回栈由
 * Navigation 3 恢复，而选中列表若只存在进程内，重启后会退回「未选中」——
 * 预览底部按钮从「取消选择」变回「选择」，缩略图条没有勾选标记，
 * 点「完成」还会把空列表报给调用方。
 *
 * 归零时机由调用方决定（每次新流程进入选择器时清空），本端口只做读写。
 */
interface PictureSelectionRepository {

    /** 读取上次保存的选中 uri 列表；没有记录时返回空列表。 */
    suspend fun loadSelectedUris(): List<String>

    /** 覆盖保存选中 uri 列表；传空列表即清空记录。 */
    suspend fun saveSelectedUris(uris: List<String>)
}
