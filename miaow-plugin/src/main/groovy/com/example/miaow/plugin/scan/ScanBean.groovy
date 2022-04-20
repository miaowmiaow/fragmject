package com.example.miaow.plugin.scan.bean

class ScanBean implements Cloneable {

    String owner
    String name
    String desc

    ScanBean() {
    }

    @Override
    protected ScanBean clone() {
        try {
            return (ScanBean) super.clone()
        } catch (CloneNotSupportedException e) {
            e.printStackTrace()
        }
        return null
    }
}