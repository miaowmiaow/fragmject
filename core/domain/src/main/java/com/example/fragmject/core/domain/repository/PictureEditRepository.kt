package com.example.fragmject.core.domain.repository

/**
 * 图片编辑结果的原图 → 新图替换记录（持久化端口）。
 *
 * 编辑保存后原图会被异步删除，而预览页的路由参数（`PicturePreviewNavKey.uris`）里仍是
 * **编辑前**的 uri——它由 Navigation 3 的返回栈在进程重建后原样恢复。只把替换关系放在
 * 进程内会随进程一起丢失，重建后的预览就会去解码一张已被删除的图（空白 / 加载失败）。
 *
 * 因此替换关系必须落库：它是「原图已删」这一事实的唯一证据。
 */
interface PictureEditRepository {

    /** 记录 [oldUri] 被编辑为 [newUri]；同一 [oldUri] 以最后一次写入为准。 */
    suspend fun recordReplacement(oldUri: String, newUri: String)

    /** 查询 [oldUri] 的替换结果；没有记录时返回 null。 */
    suspend fun findReplacement(oldUri: String): String?
}
