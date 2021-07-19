package com.example.plugin.statistic.mt

class MethodTimerEntity implements Cloneable {

    /**
     * 时间过滤
     */
    long time
    /**
     * 扫描的方法过滤
     */
    String owner

    MethodTimerEntity() {
    }

    @Override
    protected MethodTimerEntity clone() {
        try {
            return (MethodTimerEntity) super.clone()
        } catch (CloneNotSupportedException e) {
            e.printStackTrace()
        }
        return null
    }
}