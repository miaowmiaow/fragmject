package com.example.miaow.plugin.bean

import java.io.Serializable

class TraceBean(
    /**
     * 方式插入时机, 0: 方法退出前; 1: 方法进入时
     */
    var onMethod: Int = 0,
    /**
     * 埋点类名(全路径)
     */
    var traceOwner: String = "",
    /**
     * 埋点方法名
     */
    var traceName: String = "",
    /**
     * 埋点方法描述
     */
    var traceDesc: String = "",
    /**
     * 需要埋点类名(全路径)
     */
    var owner: String = "",
    /**
     * 需要埋点方法名
     */
    var name: String = "",
    /**
     * 需要埋点方法描述
     */
    var desc: String = "",
    /**
     * 需要埋点注解方法描述(全路径)
     */
    var annotationDesc: String = "",
    /**
     * 需要埋点注解参数名
     * String:注解参数名
     * String:参数类型
     */
    var annotationParams: Map<String, String> = LinkedHashMap(),
    /**
     * 需要埋点注解参数值
     * String:注解参数名
     * Object:参数值
     */
    var annotationData: MutableMap<String, Any?> = HashMap(),
) : Cloneable, Serializable {

    public override fun clone(): TraceBean {
        return try {
            super.clone() as TraceBean
        } catch (e: CloneNotSupportedException) {
            e.printStackTrace()
            TraceBean()
        }
    }

}