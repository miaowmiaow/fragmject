package com.example.miaow.base.utils

import android.util.Log
import com.example.miaow.base.BuildConfig

/**
 * 项目统一日志工具。
 *
 * 设计动机：
 * 1. 替换原本 `Log.e(tag, e.message.toString())` 的写法。
 *    - `e.message` 可能为 null，`.toString()` 会得到字符串 "null"，丢失全部异常信息；
 *    - 该写法只打印消息，丢失了堆栈，线上排查困难。
 * 2. 通过传入 `Throwable` 参数，让 logcat 输出完整堆栈，便于定位。
 * 3. 在 Release 包中可以选择性收敛日志级别，避免敏感信息泄露。
 */

/** 通用 TAG 取值：使用 Class.simpleName，避免完整包名导致 logcat 难以阅读。 */
internal val Any.logTag: String
    get() = this::class.java.simpleName.ifEmpty { this::class.java.name }

/**
 * 统一的错误日志输出。
 *
 * @param tag 日志 TAG，建议使用类名级别的常量。
 * @param msg 简要描述，方便日志检索。
 * @param tr 异常对象，必填以保留完整堆栈。
 */
fun logE(tag: String, msg: String, tr: Throwable) {
    Log.e(tag, msg, tr)
}

/**
 * 任意对象上的错误日志扩展，自动用 simpleName 作为 TAG。
 */
fun Any.logE(msg: String, tr: Throwable) {
    Log.e(logTag, msg, tr)
}

/**
 * 仅 Debug 包输出的 debug 日志。
 */
fun Any.logD(msg: String) {
    if (BuildConfig.DEBUG) {
        Log.d(logTag, msg)
    }
}
