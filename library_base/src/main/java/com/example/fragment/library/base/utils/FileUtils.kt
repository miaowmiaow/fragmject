package com.example.fragment.library.base.utils

import com.example.fragment.library.base.component.provider.BaseProvider
import java.io.File

object FileUtils {

    @JvmStatic
    fun getDir(path: String): File {
        val dir = BaseProvider.mContext.getExternalFilesDir(null)?.path
        val file = File(dir + File.separator + path)
        if (!file.exists() || !file.isDirectory) {
            file.mkdirs()
        }
        return file
    }

}