package com.example.fragment.library.base.utils

import com.google.gson.Gson
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

class GSonUtils {

    companion object {
        fun <T> fromJson(json: String, raw: Class<*>, vararg args: Type): T {
            val type = object : ParameterizedType {
                override fun getRawType(): Type = raw
                override fun getActualTypeArguments(): Array<out Type> = args
                override fun getOwnerType(): Type? = null
            }
            return Gson().fromJson(json, type)
        }
    }
}