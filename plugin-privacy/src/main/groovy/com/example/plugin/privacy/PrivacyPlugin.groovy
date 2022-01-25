package com.example.plugin.privacy

import com.android.build.gradle.AppExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

class PrivacyPlugin implements Plugin<Project> {

    public static List<PrivacyEntity> PRIVACY_FIELDS
    public static List<PrivacyEntity> PRIVACY_METHODS

    @Override
    void apply(Project project) {
        def android = project.extensions.findByType(AppExtension)
        android.registerTransform(new PrivacyTransform())
        def privacyExtension = project.extensions.create('privacy', PrivacyExtension)
        project.afterEvaluate {
            PRIVACY_FIELDS = new ArrayList<>()
            PRIVACY_METHODS = new ArrayList<>()
            def filedPoint = privacyExtension.getFiledPoint()
            if (filedPoint != null) {
                filedPoint.each { Map<String, Object> map ->
                    PrivacyEntity entity = new PrivacyEntity()
                    if (map.containsKey("owner")) {
                        entity.owner = map.get("owner")
                    }
                    if (map.containsKey("name")) {
                        entity.name = map.get("name")
                    }
                    if (map.containsKey("desc")) {
                        entity.desc = map.get("desc")
                    }
                    PRIVACY_FIELDS.add(entity)
                }
            }
            def methodPoint = privacyExtension.getMethodPoint()
            if (methodPoint != null) {
                methodPoint.each { Map<String, Object> map ->
                    PrivacyEntity entity = new PrivacyEntity()
                    if (map.containsKey("owner")) {
                        entity.owner = map.get("owner")
                    }
                    if (map.containsKey("name")) {
                        entity.name = map.get("name")
                    }
                    if (map.containsKey("desc")) {
                        entity.desc = map.get("desc")
                    }
                    PRIVACY_METHODS.add(entity)
                }
            }
        }
    }
}