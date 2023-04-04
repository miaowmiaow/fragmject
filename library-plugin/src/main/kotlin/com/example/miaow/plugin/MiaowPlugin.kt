package com.example.miaow.plugin

import com.android.build.api.instrumentation.FramesComputationMode
import com.android.build.api.instrumentation.InstrumentationScope
import com.android.build.api.variant.AndroidComponentsExtension
import com.example.miaow.plugin.asm.ScanClassVisitorFactory
import com.example.miaow.plugin.asm.TimeClassVisitorFactory
import com.example.miaow.plugin.asm.TraceClassVisitorFactory
import com.example.miaow.plugin.bean.ScanBean
import com.example.miaow.plugin.bean.TimeBean
import com.example.miaow.plugin.bean.TraceBean
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.objectweb.asm.Opcodes

class MiaowPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val androidComponents = project.extensions.getByType(AndroidComponentsExtension::class.java)
        androidComponents.onVariants { variant ->
            variant.instrumentation.transformClassesWith(
                ScanClassVisitorFactory::class.java,
                InstrumentationScope.ALL
            ) {
                it.ignoreOwner.set("com/example/fragment/library/common/utils/BuildUtils")
                it.listOfScans.set(
                    listOf(
                        ScanBean(
                            "android/os/Build",
                            "BRAND",
                            "Ljava/lang/String;",
                            Opcodes.INVOKESTATIC,
                            "com/example/fragment/library/common/utils/BuildUtils",
                            "getBrand",
                            "()Ljava/lang/String;"
                        ),
                        ScanBean(
                            "android/os/Build",
                            "MODEL",
                            "Ljava/lang/String;",
                            Opcodes.INVOKESTATIC,
                            "com/example/fragment/library/common/utils/BuildUtils",
                            "getModel",
                            "()Ljava/lang/String;"
                        ),
                        ScanBean(
                            "android/os/Build",
                            "SERIAL",
                            "Ljava/lang/String;",
                            Opcodes.INVOKESTATIC,
                            "com/example/fragment/library/common/utils/BuildUtils",
                            "getSerial",
                            "()Ljava/lang/String;"
                        ),
                        ScanBean(  //传感器检测
                            "android/hardware/SensorManager",
                            "getSensorList",
                            "(I)Ljava/util/List;"
                        ),
                    )
                )
            }
            variant.instrumentation.transformClassesWith(
                TimeClassVisitorFactory::class.java,
                InstrumentationScope.ALL
            ) {
                it.listOfTimes.set(
                    listOf(
                        TimeBean( //具体到方法名称
                            "com/example/fragment/project/activity/MainActivity",
                            "onCreate",
                            "(Landroid/os/Bundle;)V"
                        ),
                        TimeBean( //以包名和执行时间为条件
                            "com/example/fragment/library/base",
                            time = 50L
                        )
                    )
                )
            }
            variant.instrumentation.transformClassesWith(
                TraceClassVisitorFactory::class.java,
                InstrumentationScope.ALL
            ) {
                it.packageName.set("com/example/fragment")
                it.listOfTraces.set(
                    listOf(
                        TraceBean(
                            owner = "Landroid/view/View\$OnClickListener;",
                            name = "onClick",
                            desc = "(Landroid/view/View;)V",
                            traceOwner = "com/example/fragment/library/common/utils/StatisticHelper",
                            traceName = "viewOnClick",
                            traceDesc = "(Landroid/view/View;)V" //参数应在desc范围之内
                        ),
                        TraceBean(
                            annotationDesc = "Lcom/example/fragment/library/common/utils/TestAnnotation;",
                            annotationParams = mapOf(
                                //参数名 : 参数类型（对应的ASM指令，加载不同类型的参数需要不同的指令）
                                //this  : 所在方法的当前对象的引用（默认关键字，按需可选配置）
                                "this" to "Ljava/lang/Object;",
                                "code" to "I",
                                "message" to "Ljava/lang/String;"
                            ),
                            traceOwner = "com/example/fragment/library/common/utils/StatisticHelper",
                            traceName = "testAnnotation",
                            traceDesc = "(Ljava/lang/Object;ILjava/lang/String;)V" //对照annotationParams，注意参数顺序
                        ),
                    )
                )
            }
            variant.instrumentation.setAsmFramesComputationMode(FramesComputationMode.COPY_FRAMES)
        }
    }

}