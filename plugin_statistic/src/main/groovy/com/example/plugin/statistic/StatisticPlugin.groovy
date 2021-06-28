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
        def extension = project.extensions.create('buryPoint', BuryPointExtension)
        def android = project.extensions.findByType(AppExtension)
        android.registerTransform(new BuryPointTransform())
        project.afterEvaluate {
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
                    cell.methodParams = map.get("methodParams")
                    HOOKS.put(cell.methodName + cell.methodDesc, cell)
                }
            }
        }
    }
}