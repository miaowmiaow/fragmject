package com.example.miaow.plugin.bean

import java.io.Serializable

class ScanBean(
    var isMethod: Boolean = false,
    var owner: String = "",
    var name: String = "",
    var desc: String = ""
) : Cloneable, Serializable {

    public override fun clone(): ScanBean {
        return try {
            super.clone() as ScanBean
        } catch (e: CloneNotSupportedException) {
            e.printStackTrace()
            ScanBean()
        }
    }

}