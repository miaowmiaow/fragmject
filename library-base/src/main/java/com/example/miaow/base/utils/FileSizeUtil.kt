package com.example.miaow.base.utils

import android.os.Environment
import android.os.StatFs
import android.util.Log
import java.io.File
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * FileUtil 拆分文件之一：尺寸 / 可用空间。
 *
 * 这些 `internal` 顶层函数仅供 [FileUtil] 内部转发使用，外部调用方仍通过
 * `FileUtil.xxx(...)` 访问，从而保持 100% 的 API 兼容性。
 */
private const val TAG = "FileUtil"

internal fun isSDCardAliveInternal(): Boolean {
    return Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED
}

internal fun deleteInternal(file: File?) {
    file?.let {
        if (it.isDirectory) {
            it.listFiles()?.let { list ->
                for (i in list) {
                    deleteInternal(i)
                }
            }
        } else {
            it.delete()
        }
    }
}

internal fun getSizeInternal(file: File): Long {
    var size: Long = 0
    try {
        file.listFiles()?.apply {
            for (f in this) {
                size = if (f.isDirectory) {
                    size + getSizeInternal(f)
                } else {
                    size + f.length()
                }
            }
        }
    } catch (e: Exception) {
        Log.e(TAG, "getSize failed", e)
    }
    return size
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
