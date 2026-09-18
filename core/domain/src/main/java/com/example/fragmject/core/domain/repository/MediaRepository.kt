package com.example.fragmject.core.domain.repository

import com.example.fragmject.core.domain.result.MediaSaveResult

/**
 * 媒体保存领域端口。
 *
 * 抽象「保存图片/视频到系统相册」能力。保存是异步平台操作，
 * 通过 [onFinish] 回调返回结果，避免 suspend 桥接时失败路径无回调导致协程悬挂。
 *
 * 参数全部使用平台无关类型，保持 domain 层纯净：
 * - [saveImageToAlbum] url 版本：由 data 层负责下载后写入相册；
 * - [saveImageToAlbum] bytes 版本：由调用方完成 Bitmap 编码后传入字节数组；
 * - [saveBase64ImageToAlbum] base64 版本：由 data 层负责解码与重编码后写入相册；
 * - [saveVideoToAlbum] filePath 版本：传入已就绪的本地文件路径；
 * - [notifyMediaAdded] 通知系统扫描已存在的媒体文件（不复制，适用于文件已在公共目录的场景）；
 * - [createImageUri] 为拍照预创建 MediaStore Uri，返回 uri 字符串（失败返回空串）；
 * - [finishImageUri] 拍照成功后清除 pending 状态，使照片对系统相册与查询可见；
 * - [deleteImageUri] 删除预创建但未写入数据的图片记录（拍照取消/失败时清理，避免残留透明图）。
 */
interface MediaRepository {
    fun saveImageToAlbum(url: String, onFinish: (MediaSaveResult) -> Unit)
    fun saveImageToAlbum(imageBytes: ByteArray, onFinish: (MediaSaveResult) -> Unit)
    fun saveBase64ImageToAlbum(base64: String, onFinish: (MediaSaveResult) -> Unit)
    fun saveVideoToAlbum(filePath: String, onFinish: (MediaSaveResult) -> Unit)
    fun notifyMediaAdded(filePath: String, onFinish: (MediaSaveResult) -> Unit)
    fun createImageUri(): String
    fun finishImageUri(uri: String)
    fun deleteImageUri(uri: String)
}
