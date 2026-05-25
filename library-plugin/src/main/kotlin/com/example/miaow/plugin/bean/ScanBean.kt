package com.example.miaow.plugin.bean

import java.io.Serializable

class ScanBean(
    var owner: String = "",
    var name: String = "",
    var desc: String = "",
    var replaceOpcode: Int = 0,
    var replaceOwner: String = "",
    var replaceName: String = "",
    var replaceDesc: String = "",
) : Cloneable, Serializable {

    public override fun clone(): ScanBean {
        return try {
            super.clone() as ScanBean
        } catch (e: CloneNotSupportedException) {
            System.err.println("ScanBean clone failed: ${e.message}")
            ScanBean()
        }
    }

}