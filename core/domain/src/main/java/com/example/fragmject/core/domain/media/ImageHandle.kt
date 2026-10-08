package com.example.fragmject.core.domain.media

/**
 * 已加载位图的平台无关句柄。
 *
 * 刻意不暴露 android.graphics.Bitmap：领域层保持纯净，位图本体由数据层实现持有。
 *
 * 句柄本身承担两项能力，使「位图有效性校验」与「编码」都收敛在领域契约之下：
 * - [width] / [height]：供领域实现做有效性校验（如拒绝 1×1 空白图）；
 * - [obtainPngBytes]：按 PNG 编码导出字节，实现内部自行切换到合适的调度器，
 *   调用方无需关心线程。
 */
interface ImageHandle {
    val width: Int
    val height: Int

    /** 导出 PNG 字节；编码失败返回 null。 */
    suspend fun obtainPngBytes(): ByteArray?
}
