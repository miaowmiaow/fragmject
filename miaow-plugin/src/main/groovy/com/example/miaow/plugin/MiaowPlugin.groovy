package com.example.miaow.plugin

import com.android.build.gradle.AppExtension
import com.example.miaow.plugin.bury.BuryPointBean
import com.example.miaow.plugin.scan.ScanBean
import com.example.miaow.plugin.time.TimeMethodBean
import org.gradle.api.Plugin
import org.gradle.api.Project

class MiaowPlugin implements Plugin<Project> {

    public static Map<String, BuryPointBean> BURY_POINT_MAP = new HashMap<>()
    public static List<ScanBean> SCAN_FIELDS = new ArrayList<>()
    public static List<ScanBean> SCAN_METHODS = new ArrayList<>()
    public static List<TimeMethodBean> TIME_METHODS = new ArrayList<>()

    @Override
    void apply(Project project) {
        AppExtension appExtension = project.extensions.findByType(AppExtension.class)
        // 注册Transform
        appExtension.registerTransform(new MiaowTransform())
        // 获取gradle里面配置的信息
        def miaowExtension = project.extensions.create('miaow', MiaowExtension)
        project.afterEvaluate {
            // 遍历配置的埋点信息，将其保存在BURY_POINT_MAP方便调用
            miaowExtension.getBuryPoint().each {
                BuryPointBean bean = new BuryPointBean()
                if (it.containsKey("isAnnotation")) {
                    bean.isAnnotation = it.get("isAnnotation")
                }
                if (it.containsKey("isMethodExit")) {
                    bean.isMethodExit = it.get("isMethodExit")
                }
                if (it.containsKey("agentOwner")) {
                    bean.agentOwner = it.get("agentOwner")
                }
                if (it.containsKey("agentName")) {
                    bean.agentName = it.get("agentName")
                }
                if (it.containsKey("agentDesc")) {
                    bean.agentDesc = it.get("agentDesc")
                }
                if (bean.isAnnotation) {
                    if (it.containsKey("annotationDesc")) {
                        bean.annotationDesc = it.get("annotationDesc")
                    }
                    if (it.containsKey("annotationParams")) {
                        bean.annotationParams = it.get("annotationParams")
                    }
                    BURY_POINT_MAP.put(bean.annotationDesc, bean)
                } else {
                    if (it.containsKey("methodOwner")) {
                        bean.methodOwner = it.get("methodOwner")
                    }
                    if (it.containsKey("methodName")) {
                        bean.methodName = it.get("methodName")
                    }
                    if (it.containsKey("methodDesc")) {
                        bean.methodDesc = it.get("methodDesc")
                    }
                    BURY_POINT_MAP.put(bean.methodName + bean.methodDesc, bean)
                }
            }
            // 遍历配置的扫描字段信息，将其保存在SCAN_FIELDS方便调用
            miaowExtension.getScanFiled().each {
                ScanBean bean = new ScanBean()
                if (it.containsKey("owner")) {
                    bean.owner = it.get("owner")
                }
                if (it.containsKey("name")) {
                    bean.name = it.get("name")
                }
                if (it.containsKey("desc")) {
                    bean.desc = it.get("desc")
                }
                SCAN_FIELDS.add(bean)
            }
            // 遍历配置的扫描方法信息，将其保存在SCAN_METHODS方便调用
            miaowExtension.getScanMethod().each {
                ScanBean bean = new ScanBean()
                if (it.containsKey("owner")) {
                    bean.owner = it.get("owner")
                }
                if (it.containsKey("name")) {
                    bean.name = it.get("name")
                }
                if (it.containsKey("desc")) {
                    bean.desc = it.get("desc")
                }
                SCAN_METHODS.add(bean)
            }
            // 遍历配置的方法计时信息，将其保存在TIME_METHODS方便调用
            miaowExtension.getTimeMethod().each {
                TimeMethodBean bean = new TimeMethodBean()
                if (it.containsKey("time")) {
                    bean.time = it.get("time")
                }
                if (it.containsKey("owner")) {
                    bean.owner = it.get("owner")
                }
                TIME_METHODS.add(bean)
            }
        }
    }
}