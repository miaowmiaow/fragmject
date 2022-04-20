package com.example.fragment.plugin

import com.android.build.gradle.AppExtension
import com.example.fragment.plugin.bury.bean.BuryPointBean
import com.example.fragment.plugin.bury.BuryPointTransform
import com.example.fragment.plugin.scan.ScanTransform
import com.example.fragment.plugin.scan.bean.ScanBean
import com.example.fragment.plugin.time.bean.TimeMethodBean
import com.example.fragment.plugin.time.TimeMethodTransform
import org.gradle.api.Plugin
import org.gradle.api.Project

class MiaowPlugin implements Plugin<Project> {

    public static Map<String, BuryPointBean> BURY_POINT_MAP = new HashMap<>()
    public static List<ScanBean> SCAN_FIELDS = new ArrayList<>()
    public static List<ScanBean> SCAN_METHODS = new ArrayList<>()
    public static List<TimeMethodBean> TIME_METHODS = new ArrayList<>()

    @Override
    void apply(Project project) {
        def android = project.extensions.findByType(AppExtension)
        // 注册Transform
        android.registerTransform(new BuryPointTransform())
        android.registerTransform(new ScanTransform())
        android.registerTransform(new TimeMethodTransform())
        // 获取gradle里面配置的埋点信息
        def extension = project.extensions.create('miaow', MiaowExtension)
        project.afterEvaluate {
            // 遍历配置的埋点信息，将其保存在BURY_POINT_MAP方便调用
            def buryPoint = extension.getBuryPoint()
            if (buryPoint != null) {
                buryPoint.each { Map<String, Object> map ->
                    BuryPointBean bean = new BuryPointBean()
                    if (map.containsKey("isAnnotation")) {
                        bean.isAnnotation = map.get("isAnnotation")
                    }
                    if (map.containsKey("isMethodExit")) {
                        bean.isMethodExit = map.get("isMethodExit")
                    }
                    if (map.containsKey("agentOwner")) {
                        bean.agentOwner = map.get("agentOwner")
                    }
                    if (map.containsKey("agentName")) {
                        bean.agentName = map.get("agentName")
                    }
                    if (map.containsKey("agentDesc")) {
                        bean.agentDesc = map.get("agentDesc")
                    }
                    if (bean.isAnnotation) {
                        if (map.containsKey("annotationDesc")) {
                            bean.annotationDesc = map.get("annotationDesc")
                        }
                        if (map.containsKey("annotationParams")) {
                            bean.annotationParams = map.get("annotationParams")
                        }
                        BURY_POINT_MAP.put(bean.annotationDesc, bean)
                    } else {
                        if (map.containsKey("methodOwner")) {
                            bean.methodOwner = map.get("methodOwner")
                        }
                        if (map.containsKey("methodName")) {
                            bean.methodName = map.get("methodName")
                        }
                        if (map.containsKey("methodDesc")) {
                            bean.methodDesc = map.get("methodDesc")
                        }
                        BURY_POINT_MAP.put(bean.methodName + bean.methodDesc, bean)
                    }
                }
            }
            // 遍历配置的扫描字段信息，将其保存在SCAN_FIELDS方便调用
            def scanFiled = extension.getScanFiled()
            if (scanFiled != null) {
                scanFiled.each { Map<String, Object> map ->
                    ScanBean bean = new ScanBean()
                    if (map.containsKey("owner")) {
                        bean.owner = map.get("owner")
                    }
                    if (map.containsKey("name")) {
                        bean.name = map.get("name")
                    }
                    if (map.containsKey("desc")) {
                        bean.desc = map.get("desc")
                    }
                    SCAN_FIELDS.add(bean)
                }
            }
            // 遍历配置的扫描方法信息，将其保存在SCAN_METHODS方便调用
            def scanMethod = extension.getScanMethod()
            if (scanMethod != null) {
                scanMethod.each { Map<String, Object> map ->
                    ScanBean bean = new ScanBean()
                    if (map.containsKey("owner")) {
                        bean.owner = map.get("owner")
                    }
                    if (map.containsKey("name")) {
                        bean.name = map.get("name")
                    }
                    if (map.containsKey("desc")) {
                        bean.desc = map.get("desc")
                    }
                    SCAN_METHODS.add(bean)
                }
            }
            // 遍历配置的方法计时信息，将其保存在TIME_METHODS方便调用
            def methodTimer = extension.getTimeMethod()
            if (methodTimer != null) {
                methodTimer.each { Map<String, Object> map ->
                    TimeMethodBean bean = new TimeMethodBean()
                    if (map.containsKey("time")) {
                        bean.time = map.get("time")
                    }
                    if (map.containsKey("owner")) {
                        bean.owner = map.get("owner")
                    }
                    TIME_METHODS.add(bean)
                }
            }
        }
    }
}