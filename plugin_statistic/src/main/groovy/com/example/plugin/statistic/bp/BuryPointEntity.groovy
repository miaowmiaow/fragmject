package com.example.plugin.statistic.bp

class BuryPointEntity implements Cloneable {

    /**
     * 注解标识
     */
    boolean isAnnotation = false
    /**
     * 方式插入时机
     * true: 方法退出前
     * false: 方法进入时
     */
    boolean isMethodExit = false
    /**
     * Lambda表达式标识
     */
    boolean isLambda = false
    /**
     * 采集数据的方法的路径
     */
    String agentOwner
    /**
     * 插入的方法的实现接口
     */
    String agentName
    /**
     * 采集数据的方法描述
     * isAnnotation = false（参数应在methodDesc范围之内）
     * isAnnotation = true（对照annotationParams，注意参数顺序）
     */
    String agentDesc
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