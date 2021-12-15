package com.example.fragment.library.base.utils

import android.os.Environment
import java.io.File
import java.math.BigDecimal

object FileUtils {

    fun isSDCardAlive(): Boolean {
        return Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED
    }

    fun delete(file: File?) {
        if (file != null) {
            if (file.isDirectory) {
                file.listFiles()?.apply {
                    for (f in this) {
                        delete(f)
                    }
                }
            } else {
                file.delete()
            }
        }
    }

    fun getSize(file: File): Long {
        var size: Long = 0
        try {
            file.listFiles()?.apply {
                for (f in this) {
                    size = if (f.isDirectory) {
                        size + getSize(f)
                    } else {
                        size + f.length()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return size
    }

    fun formatSize(size: Double): String {
        val kiloByte = size / 1024
        if (kiloByte < 1) {
            return "0KB"
        }
        val megaByte = kiloByte / 1024
        if (megaByte < 1) {
            val result = BigDecimal(kiloByte.toString())
            return result.setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString() + "KB"
        }
        val gigaByte = megaByte / 1024
        if (gigaByte < 1) {
            val result = BigDecimal(megaByte.toString())
            return result.setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString() + "MB"
        }
        val teraBytes = gigaByte / 1024
        if (teraBytes < 1) {
            val result = BigDecimal(gigaByte.toString())
            return result.setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString() + "GB"
        }
        val result = BigDecimal(teraBytes)
        return result.setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString() + "TB"
    }

}