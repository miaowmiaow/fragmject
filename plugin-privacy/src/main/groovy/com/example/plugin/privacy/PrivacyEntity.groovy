package com.example.plugin.privacy

class BuryPointEntity implements Cloneable {

    /**
     * 插入的方法的路径
     */
    String methodOwner
    /**
     * 插入的方法名
     */
    String methodName
    /**
     * 插入的方法描述
     */
    String methodDesc
    /**
     * 扫描的方法注解名称
     */
    String annotationDesc
    /**
     * String:注解参数名
     * String:参数类型
     */
    Map<String, String> annotationParams = new LinkedHashMap<>()
    /**
     * String:注解参数名
     * Object:参数值
     */
    Map<String, Object> annotationData = new HashMap<>()

    BuryPointEntity() {
    }

    @Override
    protected BuryPointEntity clone() {
        try {
            return (BuryPointEntity) super.clone()
        } catch (CloneNotSupportedException e) {
            e.printStackTrace()
        }
        return null
    }
}