package com.example.plugin.privacy

import com.android.build.gradle.AppExtension
import com.example.plugin.statistic.bp.BuryPointEntity
import com.example.plugin.statistic.bp.BuryPointTransform
import com.example.plugin.statistic.mt.MethodTimerEntity
import com.example.plugin.statistic.mt.MethodTimerTransform
import org.gradle.api.Plugin
import org.gradle.api.Project

class PrivacyPlugin implements Plugin<Project> {

    public static Map<String, BuryPointEntity> BURY_POINT_MAP
    public static List<MethodTimerEntity> METHOD_TIMER_LIST

    @Override
    void apply(Project project) {
        def android = project.extensions.findByType(AppExtension)
        // 注册Transform
        android.registerTransform(new PrivacyTransform())
    }
}