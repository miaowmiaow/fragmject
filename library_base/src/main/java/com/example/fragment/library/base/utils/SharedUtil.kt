package com.example.fragment.library.base.utils

import android.content.Context
import com.example.fragment.library.base.provider.BaseContent

object SharedUtil {

    private const val NAME = "shared"

    fun getString(key: String, defValue: String = ""): String {
        val sp = BaseContent.get().getSharedPreferences(NAME, Context.MODE_PRIVATE)
        return sp.getString(key, defValue) ?: defValue
    }

    fun setString(key: String, value: String): String {
        val sp = BaseContent.get().getSharedPreferences(NAME, Context.MODE_PRIVATE)
        val editor = sp.edit()
        editor.putString(key, value)
        editor.apply()
        return value
    }

    fun getBoolean(key: String, defValue: Boolean = false): Boolean {
        val sp = BaseContent.get().getSharedPreferences(NAME, Context.MODE_PRIVATE)
        return sp.getBoolean(key, defValue)
    }

    fun setBoolean(key: String, value: Boolean): Boolean {
        val sp = BaseContent.get().getSharedPreferences(NAME, Context.MODE_PRIVATE)
        val editor = sp.edit()
        editor.putBoolean(key, value)
        editor.apply()
        return value
    }

    fun getInt(key: String, value: Int = 0): Int {
        val sp = BaseContent.get().getSharedPreferences(NAME, Context.MODE_PRIVATE)
        return sp.getInt(key, value)
    }

    fun setInt(key: String, value: Int): Int {
        val sp = BaseContent.get().getSharedPreferences(NAME, Context.MODE_PRIVATE)
        val editor = sp.edit()
        editor.putInt(key, value)
        editor.apply()
        return value
    }
}