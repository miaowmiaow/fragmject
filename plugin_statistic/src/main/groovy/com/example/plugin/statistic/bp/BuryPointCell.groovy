package com.example.plugin.statistic.bp

class BuryPointCell implements Cloneable {

    String agentName
    String agentDesc
    String agentParent

    boolean isAnnotation = false

    String methodName
    String methodDesc
    String methodParent
    /**
     * Integer:采集数据的方法参数起始索引（ 0：this，1+：普通参数 ）
     * Integer:参数类型对应的ASM指令，加载不同类型的参数需要不同的指令
     */
    Map<Integer, Integer> methodParams = new LinkedHashMap<>()

    String annotationDesc
    /**
     * String:注解参数名
     * Integer:参数类型对应的ASM指令，加载不同类型的参数需要不同的指令
     */
    Map<String, Integer> annotationParams = new LinkedHashMap<>()
    Map<String, Object> annotationData = new HashMap<>()

    BuryPointCell() {
    }

    @Override
    protected BuryPointCell clone() {
        try {
            return (BuryPointCell) super.clone()
        } catch (CloneNotSupportedException e) {
            e.printStackTrace()
        }
        return null
    }
}