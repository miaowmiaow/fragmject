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
                it.ignoreOwners.set(
                    listOf("com/example/fragment/project/utils/SSIDUtils")
                )
                it.listOfScans.set(
                    listOf(
                        ScanBean(
                            "android/net/wifi/WifiInfo",
                            "getSSID",
                            "()Ljava/lang/String;",
                            Opcodes.INVOKESTATIC,
                            "com/example/fragment/project/utils/SSIDUtils",
                            "getSSID",
                            "()Ljava/lang/String;"
                        ),
                        ScanBean(
                            "android/net/NetworkInfo",
                            "getExtraInfo",
                            "()Ljava/lang/String;",
                            Opcodes.INVOKESTATIC,
                            "com/example/fragment/project/utils/SSIDUtils",
                            "getSSID",
                            "()Ljava/lang/String;"
                        )
                    )
                )
            }
            variant.instrumentation.transformClassesWith(
                TimeClassVisitorFactory::class.java,
                InstrumentationScope.ALL
            ) {
                it.listOfTimes.set(
                    listOf(
                        TimeBean(
                            "com/example/fragment/project/WanActivity",
                            "onCreate",
                            "(Landroid/os/Bundle;)V"
                        ),
                        TimeBean(
                            owner = "com/example/fragment/library/base",
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
                            traceOwner = "com/example/fragment/project/utils/StatisticHelper",
                            traceName = "viewOnClick",
                            traceDesc = "(Landroid/view/View;)V", //参数应在desc范围之内
                            owner = "Landroid/view/View\$OnClickListener;",
                            name = "onClick",
                            desc = "(Landroid/view/View;)V"
                        ),
                        TraceBean(
                            traceOwner = "com/example/fragment/project/utils/StatisticHelper",
                            traceName = "testAnnotation",
                            traceDesc = "(Ljava/lang/Object;ILjava/lang/String;)V", //对照annotationParams，注意参数顺序
                            annotationDesc = "Lcom/example/fragment/project/utils/TestAnnotation;",
                            annotationParams = mapOf(
                                //参数名 : 参数类型（对应的ASM指令，加载不同类型的参数需要不同的指令）
                                //this  : 所在方法的当前对象的引用（默认关键字，按需可选配置）
                                "this" to "Ljava/lang/Object;",
                                "code" to "I",
                                "message" to "Ljava/lang/String;",
                            )
                        ),
                    )
                )
            }
            variant.instrumentation.setAsmFramesComputationMode(FramesComputationMode.COPY_FRAMES)
        }
    }

}