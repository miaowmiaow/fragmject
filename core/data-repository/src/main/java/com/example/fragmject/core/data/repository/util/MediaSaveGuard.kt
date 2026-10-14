package com.example.fragmject.core.data.repository.util

import com.example.fragmject.core.domain.result.MediaSaveResult
import kotlinx.coroutines.CancellationException
import java.io.File

/**
 * 执行媒体保存并把**预期失败**统一转换为 `success = false`。
 *
 * - 普通异常（IO、解码、权限等）→ 通过 [onFailure] 记录后返回失败结果，不向 UI 抛出；
 * - [CancellationException] 原样重抛，不触发 [onFailure]，避免把「调用方取消」误报为「保存失败」，
 *   也不破坏结构化并发（旧实现的 `catch (e: Exception)` 会把取消吞成失败结果）。
 *
 * 抽成独立函数是为了让这条语义可在纯 JVM 下测试，不依赖 Context / MediaStore / Log。
 */
internal suspend fun guardedMediaSave(
    onFailure: (Throwable) -> Unit,
    block: suspend () -> MediaSaveResult,
): MediaSaveResult = try {
    block()
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    onFailure(e)
    MediaSaveResult(success = false)
}

/**
 * 执行**交给 MediaStore 之前**的准备阶段（下载落盘、写入字节）；
 * 阶段内抛出任何异常（含协程取消）时，先删除本仓库创建的临时文件再原样重抛。
 *
 * 只覆盖交接之前：交给 `MediaStoreUtils` 之后文件由它负责删除
 * （低版本由异步扫描回调删除，此时提前删除会与扫描竞争）。
 * 仅用于本仓库自己创建的临时文件，不得用于调用方传入的现有文件（如已录制的视频）。
 */
internal inline fun <T> File.discardOnFailure(block: () -> T): T =
    try {
        block()
    } catch (e: Throwable) {
        delete()
        throw e
    }
