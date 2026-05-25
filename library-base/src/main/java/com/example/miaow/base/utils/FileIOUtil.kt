package com.example.miaow.base.utils

import android.content.Context
import android.util.Log
import com.example.miaow.base.provider.BaseContentProvider
import java.io.BufferedOutputStream
import java.io.BufferedReader
import java.io.Closeable
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.RandomAccessFile
import java.nio.channels.FileChannel
import java.nio.charset.Charset

/**
 * FileUtil 拆分文件之二：流读写。
 *
 * 顺手修复点：
 * 1. [readFileBytesRangeInternal] 之前 `seek(0)` 导致 position 参数完全失效；
 * 2. [readStreamBytesInternal] 当流提前结束时返回的 buffer 后段全为 0，已改为 `copyOf(offset)` 裁剪；
 * 3. 全部 `e.printStackTrace()` 替换为 `Log.e(TAG, msg, e)`，与项目统一日志范式保持一致。
 */
private const val TAG = "FileUtil"

internal fun writeToFileInternal(inputStream: InputStream, destFile: File, append: Boolean): Boolean {
    val outputStream: OutputStream = try {
        FileOutputStream(destFile, append)
    } catch (e: Exception) {
        Log.e(TAG, "writeToFile(stream) open failed: ${destFile.absolutePath}", e)
        // 打开输出流失败时，调用方传入的 inputStream 必须被关闭，避免句柄泄露
        quickCloseInternal(inputStream)
        return false
    }
    return writeStreamInternal(inputStream, outputStream)
}

internal fun writeToFileInternal(
    content: String,
    charset: Charset,
    destFile: File,
    append: Boolean
): Boolean {
    try {
        val data = content.toByteArray(charset)
        return writeToFileInternal(data, destFile, append)
    } catch (e: Exception) {
        Log.e(TAG, "writeToFile(string) failed: ${destFile.absolutePath}", e)
    }
    return false
}

internal fun writeToFileInternal(data: ByteArray, destFile: File, append: Boolean): Boolean {
    var bufferedOut: BufferedOutputStream? = null
    return try {
        bufferedOut = BufferedOutputStream(FileOutputStream(destFile, append))
        bufferedOut.write(data)
        bufferedOut.flush()
        true
    } catch (e: Exception) {
        Log.e(TAG, "writeToFile(bytes) failed: ${destFile.absolutePath}", e)
        false
    } finally {
        quickCloseInternal(bufferedOut)
    }
}

internal fun writeStreamInternal(inputStream: InputStream, outputStream: OutputStream): Boolean {
    return if (inputStream is FileInputStream && outputStream is FileOutputStream) {
        var fis: FileChannel? = null
        var fos: FileChannel? = null
        try {
            fis = inputStream.channel
            fos = outputStream.channel
            fis.transferTo(0, fis.size(), fos) > 0
        } catch (e: Exception) {
            Log.e(TAG, "writeStream channel failed", e)
            false
        } finally {
            quickCloseInternal(fis)
            quickCloseInternal(fos)
        }
    } else {
        val buf = ByteArray(2048)
        var len: Int
        try {
            while (inputStream.read(buf).also { len = it } != -1) {
                outputStream.write(buf, 0, len)
                outputStream.flush()
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "writeStream copy failed", e)
            false
        } finally {
            quickCloseInternal(inputStream)
            quickCloseInternal(outputStream)
        }
    }
}

internal fun readFileBytesInternal(file: File): ByteArray? {
    var fis: FileInputStream? = null
    return try {
        fis = FileInputStream(file)
        readStreamBytesInternal(fis)
    } catch (e: Exception) {
        Log.e(TAG, "readFileBytes failed: ${file.absolutePath}", e)
        null
    } finally {
        // 之前只在 catch 里关闭 fis，成功路径会泄露文件句柄。
        quickCloseInternal(fis)
    }
}

internal fun readFileBytesRangeInternal(file: File, position: Int, length: Int): ByteArray? {
    if (position < 0 || length <= 0) {
        return null
    }
    var accessFile: RandomAccessFile? = null
    return try {
        accessFile = RandomAccessFile(file, "r")
        // 之前是 seek(0)，position 参数完全没生效，这里修正为按 position 偏移读取
        accessFile.seek(position.toLong())
        val reads = ByteArray(length)
        val read = accessFile.read(reads)
        if (read <= 0) {
            null
        } else if (read == length) {
            reads
        } else {
            // 读取长度可能小于请求长度（已到文件末尾），返回截断后的真实数据
            reads.copyOf(read)
        }
    } catch (e: Exception) {
        Log.e(TAG, "readFileBytes(range) failed: ${file.absolutePath}", e)
        null
    } finally {
        quickCloseInternal(accessFile)
    }
}

internal fun readStreamBytesInternal(inputStream: InputStream): ByteArray? {
    return readStreamBytesInternal(inputStream, -1)
}

internal fun readStreamBytesInternal(inputStream: InputStream?, readCount: Int): ByteArray? {
    if (inputStream == null) {
        return null
    }
    var count = readCount
    try {
        if (count <= 0) {
            count = inputStream.available()
        }
        val buffer = ByteArray(count)
        var temp: Int
        var offset = 0
        var maxTime = 10000
        while (offset < count) {
            if (maxTime < 0) {
                throw IOException("failed to complete after 10000 reads;")
            }
            temp = inputStream.read(buffer, offset, count - offset)
            if (temp < 0) {
                break
            }
            offset += temp
            maxTime--
        }
        // 之前直接返回 buffer，当流末尾提前结束时尾部全为 0，
        // 这里仅返回真实读取到的字节数。
        return if (offset == count) buffer else buffer.copyOf(offset)
    } catch (e: Exception) {
        Log.e(TAG, "readStreamBytes failed", e)
        quickCloseInternal(inputStream)
    }
    return null
}

internal fun readAssetStringInternal(fileName: String): String =
    readAssetStringInternal(BaseContentProvider.context(), fileName)

internal fun readAssetStringInternal(context: Context, fileName: String): String {
    val sb = StringBuilder()
    var inputStream: InputStream? = null
    var bufferedReader: BufferedReader? = null
    try {
        inputStream = context.assets.open(fileName)
        bufferedReader = BufferedReader(InputStreamReader(inputStream))
        var line: String?
        do {
            line = bufferedReader.readLine()
            if (line != null) {
                sb.append(line)
            }
        } while (line != null)
    } catch (e: Exception) {
        Log.e(TAG, "readAssetString failed: $fileName", e)
    } finally {
        // 之前只在 catch 里关闭流，成功路径会泄露 asset 文件句柄。
        quickCloseInternal(bufferedReader)
        quickCloseInternal(inputStream)
    }
    return sb.toString()
}

internal fun quickCloseInternal(closeable: Closeable?) {
    if (closeable != null) {
        try {
            closeable.close()
        } catch (e: Exception) {
            Log.e(TAG, "quickClose failed", e)
        }
    }
}
