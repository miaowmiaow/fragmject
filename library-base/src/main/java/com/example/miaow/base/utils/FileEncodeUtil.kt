package com.example.miaow.base.utils

import android.util.Base64
import java.nio.charset.Charset

/**
 * FileUtil 拆分文件之四：编码 / Base64 / 二进制串。
 */
private const val BINARY_SEPARATOR = " "

// 字符串转换为二进制字符串
internal fun strToBinaryInternal(str: String): String {
    val sb = StringBuilder()
    val bytes = str.toByteArray()
    for (aByte in bytes) {
        sb.append(Integer.toBinaryString(aByte.toInt())).append(BINARY_SEPARATOR)
    }
    return sb.toString()
}

// 二进制字符串转换为普通字符串
internal fun binaryToStrInternal(binaryStr: String): String {
    val sb = StringBuilder()
    val binArrays = binaryStr.split(BINARY_SEPARATOR.toRegex()).dropLastWhile { it.isEmpty() }
        .toTypedArray()
    for (binStr in binArrays) {
        val c = binaryToChar(binStr)
        sb.append(c)
    }
    return sb.toString()
}

// 二进制字符转换为 int 数组
private fun binaryToIntArray(binaryStr: String): IntArray {
    val temp = binaryStr.toCharArray()
    val result = IntArray(temp.size)
    for (i in temp.indices) {
        result[i] = temp[i].code - 48
    }
    return result
}

// 将二进制转换成字符
private fun binaryToChar(binaryStr: String): Char {
    val temp = binaryToIntArray(binaryStr)
    var sum = 0
    for (i in temp.indices) {
        sum += temp[temp.size - 1 - i] shl i
    }
    return sum.toChar()
}

internal fun encodeBase64Internal(bytes: ByteArray): String {
    return Base64.encodeToString(bytes, Base64.DEFAULT)
}

internal fun encodeBinaryInternal(bytes: ByteArray, charset: Charset): String {
    return binaryToStrInternal(String(bytes, charset))
}

internal fun encodeBytesInternal(bytes: ByteArray, charset: Charset): String {
    return String(bytes, charset)
}

internal fun decodeBase64Internal(content: String): ByteArray? {
    return Base64.decode(content, Base64.DEFAULT)
}

internal fun decodeBinaryInternal(content: String, charset: Charset): ByteArray {
    val binaryString = strToBinaryInternal(content)
    return decodeStringInternal(binaryString, charset)
}

internal fun decodeStringInternal(content: String, charset: Charset): ByteArray {
    return content.toByteArray(charset)
}
