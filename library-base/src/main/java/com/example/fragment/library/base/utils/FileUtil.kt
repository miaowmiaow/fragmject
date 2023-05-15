package com.example.fragment.library.base.utils

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Base64
import android.webkit.MimeTypeMap
import java.io.*
import java.math.BigDecimal
import java.math.RoundingMode
import java.nio.channels.FileChannel
import java.nio.charset.Charset
import java.nio.file.Files
import java.util.*

object FileUtil {

    fun isSDCardAlive(): Boolean {
        return Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED
    }

    fun delete(file: File?) {
        file?.let {
            if (it.isDirectory) {
                it.listFiles()?.let { list ->
                    for (i in list) {
                        delete(i)
                    }
                }
            } else {
                it.delete()
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
            return result.setScale(2, RoundingMode.HALF_UP).toPlainString() + "KB"
        }
        val gigaByte = megaByte / 1024
        if (gigaByte < 1) {
            val result = BigDecimal(megaByte.toString())
            return result.setScale(2, RoundingMode.HALF_UP).toPlainString() + "MB"
        }
        val teraBytes = gigaByte / 1024
        if (teraBytes < 1) {
            val result = BigDecimal(gigaByte.toString())
            return result.setScale(2, RoundingMode.HALF_UP).toPlainString() + "GB"
        }
        val result = BigDecimal(teraBytes)
        return result.setScale(2, RoundingMode.HALF_UP).toPlainString() + "TB"
    }

    private const val BINARY_SEPARATOR = " "

    //字符串转换为二进制字符串
    fun strToBinary(str: String): String {
        val sb = StringBuilder()
        val bytes = str.toByteArray()
        for (aByte in bytes) {
            sb.append(Integer.toBinaryString(aByte.toInt())).append(BINARY_SEPARATOR)
        }
        return sb.toString()
    }

    //二进制字符串转换为普通字符串
    fun binaryToStr(binaryStr: String): String {
        val sb = StringBuilder()
        val binArrays = binaryStr.split(BINARY_SEPARATOR.toRegex()).dropLastWhile { it.isEmpty() }
            .toTypedArray()
        for (binStr in binArrays) {
            val c = binaryToChar(binStr)
            sb.append(c)
        }
        return sb.toString()
    }

    //二进制字符转换为int数组
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

    fun writeToFile(inputStream: InputStream, destFile: File, append: Boolean): Boolean {
        var outputStream: OutputStream? = null
        try {
            outputStream = FileOutputStream(destFile, append)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return if (outputStream == null) {
            false
        } else writeStream(inputStream, outputStream)
    }

    fun writeToFile(
        content: String,
        charset: Charset = Charset.defaultCharset(),
        destFile: File,
        append: Boolean
    ): Boolean {
        try {
            val data = content.toByteArray(charset)
            return writeToFile(data, destFile, append)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }

    fun writeToFile(data: ByteArray, destFile: File, append: Boolean): Boolean {
        var bufferedOut: BufferedOutputStream? = null
        return try {
            bufferedOut = BufferedOutputStream(FileOutputStream(destFile, append))
            bufferedOut.write(data)
            bufferedOut.flush()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            quickClose(bufferedOut)
        }
    }

    fun writeStream(inputStream: InputStream, outputStream: OutputStream): Boolean {
        return if (inputStream is FileInputStream && outputStream is FileOutputStream) {
            var fis: FileChannel? = null
            var fos: FileChannel? = null
            try {
                fis = inputStream.channel
                fos = outputStream.channel
                fis.transferTo(0, fis.size(), fos) > 0
            } catch (e: Exception) {
                e.printStackTrace()
                false
            } finally {
                quickClose(fis)
                quickClose(fos)
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
                e.printStackTrace()
                false
            } finally {
                quickClose(inputStream)
                quickClose(outputStream)
            }
        }
    }

    fun readFileBytes(file: File): ByteArray? {
        var fis: FileInputStream? = null
        try {
            fis = FileInputStream(file)
            return readStreamBytes(fis)
        } catch (e: Exception) {
            e.printStackTrace()
            quickClose(fis)
        }
        return null
    }

    fun readFileBytes(file: File, position: Int, length: Int): ByteArray? {
        if (position < 0 || length <= 0) {
            return null
        }
        var accessFile: RandomAccessFile? = null
        return try {
            accessFile = RandomAccessFile(file, "r")
            accessFile.seek(0)
            val reads = ByteArray(length)
            accessFile.read(reads)
            reads
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            quickClose(accessFile)
        }
    }

    fun readStreamBytes(inputStream: InputStream): ByteArray? {
        return readStreamBytes(inputStream, -1)
    }

    fun readStreamBytes(inputStream: InputStream?, readCount: Int): ByteArray? {
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
            return buffer
        } catch (e: Exception) {
            e.printStackTrace()
            quickClose(inputStream)
        }
        return null
    }

    fun readAssetString(context: Context, fileName: String): String {
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
            e.printStackTrace()
            quickClose(inputStream)
            quickClose(bufferedReader)
        }
        return sb.toString()
    }

    fun quickClose(closeable: Closeable?) {
        if (closeable != null) {
            try {
                closeable.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun encodeBase64(bytes: ByteArray): String {
        return Base64.encodeToString(bytes, Base64.DEFAULT)
    }

    fun encodeBinary(bytes: ByteArray, charset: Charset = Charset.defaultCharset()): String {
        return binaryToStr(String(bytes, charset))
    }

    fun encodeBytes(bytes: ByteArray, charset: Charset = Charset.defaultCharset()): String {
        return String(bytes, charset)
    }

    fun decodeBase64(content: String): ByteArray? {
        return Base64.decode(content, Base64.DEFAULT)
    }

    fun decodeBinary(content: String, charset: Charset = Charset.defaultCharset()): ByteArray {
        val binaryString = strToBinary(content)
        return decodeString(binaryString, charset)
    }

    fun decodeString(content: String, charset: Charset = Charset.defaultCharset()): ByteArray {
        return content.toByteArray(charset)
    }

    private fun bytesToHexString(src: ByteArray?): String {
        val sb = StringBuilder()
        if (src != null && src.isNotEmpty()) {
            for (b in src) {
                val v = b.toInt() and 0xFF
                val hv = Integer.toHexString(v)
                if (hv.length < 2) {
                    sb.append(0)
                }
                sb.append(hv)
            }
        }
        return sb.toString()
    }

    fun parseHeadCode(headCode: String): String {
        val head = headCode.uppercase(Locale.getDefault())
        return if (head.startsWith("FFD8FF")) {
            "image/jpeg"
        } else if (head.startsWith("89504E")) {
            "image/png"
        } else if (head.startsWith("474946")) {
            "image/gif"
        } else if (head.startsWith("524946")) {
            "image/webp"
        } else if (head.startsWith("49492A00")) {
            "image/tiff"
        } else if (head.startsWith("424D")) {
            "image/bmp"
        } else if (head.startsWith("3C3F786D6C")) {
            "application/xml"
        } else if (head.startsWith("68746D6C3E")) {
            "text/html"
        } else if (head.startsWith("255044462D312E")) {
            "application/pdf"
        } else if (head.startsWith("504B0304")) {
            "application/zip"
        } else if (head.startsWith("52617221")) {
            "application/rar"
        } else if (head.startsWith("57415645")) {
            "audio/x-wav"
        } else if (head.startsWith("41564920")) {
            "video/x-msvideo"
        } else if (head.startsWith("2E524D46")) {
            "application/vnd.rn-realmedia"
        } else if (head.startsWith("000001B")) {
            "video/mpeg"
        } else {
            "*/*"
        }
    }

    fun readFileHeadString(file: File): String {
        val inputStream = try {
            FileInputStream(file)
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            null
        }
        val bytes = readStreamBytes(inputStream, 16)
        return bytesToHexString(bytes)
    }

    fun readFileHeadString(context: Context, fileUri: Uri): String {
        val inputStream = try {
            context.contentResolver.openInputStream(fileUri)
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            null
        }
        val bytes = readStreamBytes(inputStream, 16)
        return bytesToHexString(bytes)
    }

    fun getFileTypeCode(file: File): String {
        return parseHeadCode(readFileHeadString(file))
    }

    fun getFileTypeCode(context: Context, fileUri: Uri): String {
        return parseHeadCode(readFileHeadString(context, fileUri))
    }

    fun getFileMimeType(context: Context, fileUri: Uri): String {
        var mimeType: String? = null
        val mmr = MediaMetadataRetriever()
        try {
            mmr.setDataSource(context, fileUri)
            mimeType = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
        if (mimeType != null) {
            return mimeType
        }
        val resolver = context.contentResolver
        try {
            mimeType = resolver.getType(fileUri)
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
        if (mimeType != null) {
            return mimeType
        }
        try {
            mimeType = parseHeadCode(readFileHeadString(context, fileUri))
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return mimeType ?: "*/*"
    }

    fun getFileMimeType(file: File): String {
        if (!file.isFile) {
            return "*/*"
        }
        var mimeType: String? = null
        val filePath = file.absolutePath
        val suffix = MimeTypeMap.getFileExtensionFromUrl(filePath)
        try {
            mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(suffix)
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
        if (mimeType != null) {
            return mimeType
        }
        val mmr = MediaMetadataRetriever()
        try {
            mmr.setDataSource(filePath)
            mimeType = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
        if (mimeType != null) {
            return mimeType
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                mimeType = Files.probeContentType(file.toPath())
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        if (mimeType != null) {
            return mimeType
        }
        try {
            mimeType = getFileTypeCode(file)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return mimeType ?: "*/*"
    }
}