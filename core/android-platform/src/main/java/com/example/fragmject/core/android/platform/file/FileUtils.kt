package com.example.fragmject.core.android.platform.file

import android.content.Context
import android.net.Uri
import java.io.Closeable
import java.io.File

/**
 * 文件工具门面对象。
 *
 * ## 设计动机
 * 历史上 `FileUtils.kt` 单文件累计到 685 行 / 59 KB，糅合了尺寸、IO、MIME、
 * 编码、脏数据测试 5 类完全独立的职责。本次重构采用「门面 + 内部顶层函数」
 * 的方式拆分：
 *
 * - 公开入口仍只挂在 `FileUtils` 这一个 `object` 上，**所有调用方零改动**；
 * - 内部按职责拆到 `internal` 顶层文件（原编码类已因零调用整体移除）：
 *   - [FileSizeUtils.kt]：尺寸 / 可用空间
 *   - [FileIOUtils.kt]：流读写
 *   - [FileMimeUtils.kt]：MIME / 文件头
 *
 * 顺手修复的真实 Bug 详见各拆分文件 KDoc。
 *
 * 门面只保留实际有调用点的方法；Base64 / 二进制串一类零调用的转发已删除，
 * 其实现文件 [FileEncodeUtils.kt] 一并移除，避免继续堆积无人使用的公开 API。
 */
object FileUtils {

    // ---------- 尺寸 / 可用空间 ----------

    fun isSDCardAlive(): Boolean = isSDCardAliveInternal()

    fun delete(file: File?) = deleteInternal(file)

    fun getSize(file: File): Long = getSizeInternal(file)

    fun formatSize(size: Double): String = formatSizeInternal(size)

    // ---------- 流读写 ----------

    fun readAssetString(context: Context, fileName: String): String =
        readAssetStringInternal(context, fileName)

    fun quickClose(closeable: Closeable?) = quickCloseInternal(closeable)

    // ---------- MIME / 文件头 ----------

    fun getFileMimeType(context: Context, fileUri: Uri): String =
        getFileMimeTypeInternal(context, fileUri)

    fun getFileMimeType(file: File): String = getFileMimeTypeInternal(file)
}
