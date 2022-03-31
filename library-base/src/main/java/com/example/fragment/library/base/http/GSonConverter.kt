package com.example.fragment.library.base.http

import com.google.gson.Gson
import okhttp3.ResponseBody

class GSonConverter : SimpleHttp.Converter {

    companion object {
        fun create(): GSonConverter {
            return GSonConverter()
        }
    }

    private val gSon = Gson()

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