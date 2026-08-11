package com.example.fragmject.core.network.http

import com.example.fragmject.core.network.utils.GSonUtils
import com.google.gson.TypeAdapter
import com.google.gson.reflect.TypeToken
import okhttp3.ResponseBody
import java.lang.reflect.Type

class GSonConverter : CoroutineHttp.Converter {

    companion object {
        fun create(): GSonConverter {
            return GSonConverter()
        }
    }

    private val gson = GSonUtils.gson

    override fun <T> converter(responseBody: ResponseBody, typeOfT: Type): T {
        val jsonReader = gson.newJsonReader(responseBody.charStream())
        @Suppress("UNCHECKED_CAST")
        val adapter = gson.getAdapter(TypeToken.get(typeOfT)) as TypeAdapter<T>
        return responseBody.use {
            adapter.read(jsonReader)
        }
    }

    override fun <T> fromJson(json: String, typeOfT: Type): T {
        return gson.fromJson(json, typeOfT)
    }

}