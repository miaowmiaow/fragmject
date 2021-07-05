package com.example.plugin.statistic

import com.android.build.gradle.AppExtension
import com.example.plugin.statistic.bp.BuryPointCell
import com.example.plugin.statistic.bp.BuryPointExtension
import com.example.plugin.statistic.bp.BuryPointTransform
import org.gradle.api.Plugin
import org.gradle.api.Project

class StatisticPlugin implements Plugin<Project> {

    public final static HashMap<String, BuryPointCell> HOOKS = new HashMap<>()

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
                boolean isAnnotation = map.get("isAnnotation")
                cell.isAnnotation = isAnnotation
                cell.agentName = map.get("agentName")
                cell.agentDesc = map.get("agentDesc")
                cell.agentParent = map.get("agentParent")
                if (isAnnotation) {
                    cell.annotationDesc = map.get("annotationDesc")
                    cell.annotationParams = map.get("annotationParams")
                    HOOKS.put(cell.annotationDesc, cell)
                } else {
                    cell.methodName = map.get("methodName")
                    cell.methodDesc = map.get("methodDesc")
                    cell.methodParent = map.get("methodParent")
                    HOOKS.put(cell.methodName + cell.methodDesc, cell)
                }
            }
        }
    }
}