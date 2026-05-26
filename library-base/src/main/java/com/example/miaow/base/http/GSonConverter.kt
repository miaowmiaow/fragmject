package com.example.miaow.base.http

import com.example.miaow.base.utils.GSonUtils
import okhttp3.ResponseBody

class GSonConverter : CoroutineHttp.Converter {

    companion object {
        fun create(): GSonConverter {
            return GSonConverter()
        }
    }

    private val gSon = GSonUtils.gson

    override fun <T> converter(responseBody: ResponseBody, type: Class<T>): T {
        val jsonReader = gSon.newJsonReader(responseBody.charStream())
        val adapter = gSon.getAdapter(type)
        return responseBody.use {
            adapter.read(jsonReader)
        }
    }

    override fun <T> fromJson(json: String, classOfT: Class<T>): T {
        return gSon.fromJson(json, classOfT)
    }

}