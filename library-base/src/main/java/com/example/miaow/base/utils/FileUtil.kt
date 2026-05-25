package com.example.miaow.base.utils

import android.content.Context
import android.net.Uri
import androidx.annotation.VisibleForTesting
import java.io.Closeable
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.Charset

/**
 * 文件工具门面对象。
 *
 * ## 设计动机
 * 历史上 `FileUtil.kt` 单文件累计到 685 行 / 59 KB，糅合了尺寸、IO、MIME、
 * 编码、脏数据测试 5 类完全独立的职责。本次重构采用「门面 + 内部顶层函数」
 * 的方式拆分：
 *
 * - 公开入口仍只挂在 `FileUtil` 这一个 `object` 上，**所有调用方零改动**；
 * - 内部按职责拆到 5 个 `internal` 顶层文件：
 *   - [FileSizeUtil.kt]：尺寸 / 可用空间
 *   - [FileIOUtil.kt]：流读写
 *   - [FileMimeUtil.kt]：MIME / 文件头
 *   - [FileEncodeUtil.kt]：Base64 / 二进制串
 *   - [FileDirtyTestUtil.kt]：脏数据写入测试
 *
 * 顺手修复的真实 Bug 详见各拆分文件 KDoc。
 */
object FileUtil {

    // ---------- 尺寸 / 可用空间 ----------

    fun isSDCardAlive(): Boolean = isSDCardAliveInternal()

    fun delete(file: File?) = deleteInternal(file)

    fun getSize(file: File): Long = getSizeInternal(file)

    fun formatSize(size: Double): String = formatSizeInternal(size)

    fun getAvailableStorage(): Long = getAvailableStorageInternal()

    // ---------- 流读写 ----------

    fun writeToFile(inputStream: InputStream, destFile: File, append: Boolean): Boolean =
        writeToFileInternal(inputStream, destFile, append)

    fun writeToFile(
        content: String,
        charset: Charset = Charset.defaultCharset(),
        destFile: File,
        append: Boolean,
    ): Boolean = writeToFileInternal(content, charset, destFile, append)

    fun writeToFile(data: ByteArray, destFile: File, append: Boolean): Boolean =
        writeToFileInternal(data, destFile, append)

    fun writeStream(inputStream: InputStream, outputStream: OutputStream): Boolean =
        writeStreamInternal(inputStream, outputStream)

    fun readFileBytes(file: File): ByteArray? = readFileBytesInternal(file)

    fun readFileBytes(file: File, position: Int, length: Int): ByteArray? =
        readFileBytesRangeInternal(file, position, length)

    fun readStreamBytes(inputStream: InputStream): ByteArray? = readStreamBytesInternal(inputStream)

    fun readStreamBytes(inputStream: InputStream?, readCount: Int): ByteArray? =
        readStreamBytesInternal(inputStream, readCount)

    fun readAssetString(fileName: String): String = readAssetStringInternal(fileName)

    fun readAssetString(context: Context, fileName: String): String =
        readAssetStringInternal(context, fileName)

    fun quickClose(closeable: Closeable?) = quickCloseInternal(closeable)

    // ---------- MIME / 文件头 ----------

    fun parseHeadCode(headCode: String): String = parseHeadCodeInternal(headCode)

    fun readFileHeadString(file: File): String = readFileHeadStringInternal(file)

    fun readFileHeadString(context: Context, fileUri: Uri): String =
        readFileHeadStringInternal(context, fileUri)

    fun getFileTypeCode(file: File): String = getFileTypeCodeInternal(file)

    fun getFileTypeCode(context: Context, fileUri: Uri): String =
        getFileTypeCodeInternal(context, fileUri)

    fun getFileMimeType(context: Context, fileUri: Uri): String =
        getFileMimeTypeInternal(context, fileUri)

    fun getFileMimeType(file: File): String = getFileMimeTypeInternal(file)

    // ---------- 编码 / Base64 / 二进制串 ----------

    fun strToBinary(str: String): String = strToBinaryInternal(str)

    fun binaryToStr(binaryStr: String): String = binaryToStrInternal(binaryStr)

    fun encodeBase64(bytes: ByteArray): String = encodeBase64Internal(bytes)

    fun encodeBinary(bytes: ByteArray, charset: Charset = Charset.defaultCharset()): String =
        encodeBinaryInternal(bytes, charset)

    fun encodeBytes(bytes: ByteArray, charset: Charset = Charset.defaultCharset()): String =
        encodeBytesInternal(bytes, charset)

    fun decodeBase64(content: String): ByteArray? = decodeBase64Internal(content)

    fun decodeBinary(content: String, charset: Charset = Charset.defaultCharset()): ByteArray =
        decodeBinaryInternal(content, charset)

    fun decodeString(content: String, charset: Charset = Charset.defaultCharset()): ByteArray =
        decodeStringInternal(content, charset)

    // ---------- 脏数据写入测试（仅用于本地存储压力调试） ----------

    @VisibleForTesting
    fun writeDirtyRead(destFile: File) = writeDirtyReadInternal(destFile)
}