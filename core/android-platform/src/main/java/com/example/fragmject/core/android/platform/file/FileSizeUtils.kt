package com.example.fragmject.core.android.platform.file

import android.os.Environment
import android.os.StatFs
import android.util.Log
import java.io.File
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * FileUtils 拆分文件之一：尺寸 / 可用空间。
 *
 * 这些 `internal` 顶层函数仅供 [FileUtils] 内部转发使用，外部调用方仍通过
 * `FileUtils.xxx(...)` 访问，从而保持 100% 的 API 兼容性。
 */
private const val TAG = "FileUtils"

internal fun isSDCardAliveInternal(): Boolean {
    return Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED
}

internal fun deleteInternal(file: File?) {
    if (file == null) return
    if (!file.isDirectory) {
        file.delete()
        return
    }
    file.listFiles()?.forEach { deleteInternal(it) }
}

internal fun getSizeInternal(file: File): Long {
    val children = try {
        file.listFiles()
    } catch (e: Exception) {
        Log.e(TAG, "getSize failed", e)
        null
    } ?: return 0
    return children.sumOf { child ->
        if (child.isDirectory) getSizeInternal(child) else child.length()
    }
}

internal fun formatSizeInternal(size: Double): String {
    val kiloByte = size / 1000
    if (kiloByte < 1) {
        return "0KB"
    }
    val megaByte = kiloByte / 1000
    if (megaByte < 1) {
        val result = BigDecimal(kiloByte.toString())
        return result.setScale(2, RoundingMode.HALF_UP).toPlainString() + "KB"
    }
    val gigaByte = megaByte / 1000
    if (gigaByte < 1) {
        val result = BigDecimal(megaByte.toString())
        return result.setScale(2, RoundingMode.HALF_UP).toPlainString() + "MB"
    }
    val teraBytes = gigaByte / 1000
    if (teraBytes < 1) {
        val result = BigDecimal(gigaByte.toString())
        return result.setScale(2, RoundingMode.HALF_UP).toPlainString() + "GB"
    }
    val result = BigDecimal(teraBytes)
    return result.setScale(2, RoundingMode.HALF_UP).toPlainString() + "TB"
}

internal fun getAvailableStorageInternal(): Long {
    val stat = StatFs(Environment.getExternalStorageDirectory().path)
    return stat.blockSizeLong * stat.availableBlocksLong
}
