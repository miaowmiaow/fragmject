package com.example.miaow.plugin.bean

import java.io.Serializable

class TimeBean(
    val owner: String = "",
    val name: String = "",
    val desc: String = "",
    val time: Long = 0L
) : Cloneable, Serializable {

    public override fun clone(): TimeBean {
        return try {
            super.clone() as TimeBean
        } catch (e: CloneNotSupportedException) {
            e.printStackTrace()
            TimeBean()
        }
    }

}
