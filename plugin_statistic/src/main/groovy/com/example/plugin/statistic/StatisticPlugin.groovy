package com.example.plugin.statistic

import com.android.build.gradle.AppExtension
import com.example.plugin.statistic.bp.BuryPointCell
import com.example.plugin.statistic.bp.BuryPointExtension
import com.example.plugin.statistic.bp.BuryPointTransform
import org.gradle.api.Plugin
import org.gradle.api.Project

class StatisticPlugin implements Plugin<Project> {

    public final static Map<String, BuryPointCell> HOOKS = new HashMap<>()

    @Override
    void apply(Project project) {
        def android = project.extensions.findByType(AppExtension)
        // 注册BuryPointTransform
        android.registerTransform(new BuryPointTransform())
        // 获取gradle里面配置的埋点信息
        def extension = project.extensions.create('buryPoint', BuryPointExtension)
        project.afterEvaluate {
            // 遍历配置的埋点信息，将其保存在HOOKS方便调用
            extension.hooks.each { Map<String, Object> map ->
                BuryPointCell cell = new BuryPointCell()
                if(map.containsKey("isAnnotation")){
                    cell.isAnnotation = map.get("isAnnotation")
                }
                if(map.containsKey("isMethodExit")){
                    cell.isMethodExit = map.get("isMethodExit")
                }
                if(map.containsKey("agentName")){
                    cell.agentName = map.get("agentName")
                }
                if(map.containsKey("agentDesc")){
                    cell.agentDesc = map.get("agentDesc")
                }
                if(map.containsKey("agentParent")){
                    cell.agentParent = map.get("agentParent")
                }
                if (cell.isAnnotation) {
                    if(map.containsKey("annotationDesc")){
                        cell.annotationDesc = map.get("annotationDesc")
                    }
                    if(map.containsKey("annotationParams")){
                        cell.annotationParams = map.get("annotationParams")
                    }
                    HOOKS.put(cell.annotationDesc, cell)
                } else {
                    if(map.containsKey("methodName")){
                        cell.methodName = map.get("methodName")
                    }
                    if(map.containsKey("methodDesc")){
                        cell.methodDesc = map.get("methodDesc")
                    }
                    if(map.containsKey("methodParent")){
                        cell.methodParent = map.get("methodParent")
                    }
                    HOOKS.put(cell.methodName + cell.methodDesc, cell)
                }
            }
        }
    }
}