package com.example.miaow.plugin.time

class TimeMethodBean implements Cloneable {

    /**
     * 时间过滤
     */
    long time
    /**
     * 扫描的方法过滤
     */
    String owner

    TimeMethodBean() {
    }

    @Override
    protected TimeMethodBean clone() {
        try {
            return (TimeMethodBean) super.clone()
        } catch (CloneNotSupportedException e) {
            e.printStackTrace()
        }
        return null
    }
}