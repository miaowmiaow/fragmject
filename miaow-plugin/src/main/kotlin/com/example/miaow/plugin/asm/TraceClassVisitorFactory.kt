package com.example.miaow.plugin.asm

import com.android.build.api.instrumentation.AsmClassVisitorFactory
import com.android.build.api.instrumentation.ClassContext
import com.android.build.api.instrumentation.ClassData
import com.android.build.api.instrumentation.InstrumentationParameters
import com.example.miaow.plugin.bean.TraceBean
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.objectweb.asm.*
import org.objectweb.asm.commons.AdviceAdapter

interface TraceParams : InstrumentationParameters {
    @get:Input
    val packageName: Property<String>

    @get:Input
    val listOfTraces: ListProperty<TraceBean>
}

abstract class TraceClassVisitorFactory : AsmClassVisitorFactory<TraceParams> {

    override fun createClassVisitor(
        classContext: ClassContext,
        nextClassVisitor: ClassVisitor
    ): ClassVisitor {
        return TraceClassVisitor(nextClassVisitor, parameters.get().listOfTraces.get())
    }

    override fun isInstrumentable(classData: ClassData): Boolean {
        return classData.className.startsWith(parameters.get().packageName.get().replace("/", "."))
    }

}

class TraceClassVisitor(
    classVisitor: ClassVisitor,
    private val traces: MutableList<TraceBean>
) : ClassVisitor(Opcodes.ASM9, classVisitor) {

    private var isInterface = false
    private val traceMap: MutableMap<String, TraceBean> = HashMap()

    override fun visit(
        version: Int,
        access: Int,
        name: String?,
        signature: String?,
        superName: String?,
        interfaces: Array<out String>?
    ) {
        super.visit(version, access, name, signature, superName, interfaces)
        isInterface = access and Opcodes.ACC_INTERFACE != 0
        traces.forEach {
            if (it.annotationDesc.isNotBlank()) {
                traceMap[it.annotationDesc] = it
            } else {
                traceMap[it.name + it.desc] = it
            }
        }
    }

    /**
     * 扫描类的方法进行调用
     * @param access 修饰符
     * @param name 方法名字
     * @param descriptor 方法签名
     * @param signature 泛型信息
     * @param exceptions 抛出的异常
     * @return
     */
    override fun visitMethod(
        access: Int,
        name: String,
        descriptor: String?,
        signature: String?,
        exceptions: Array<String>?
    ): MethodVisitor {
        var methodVisitor = super.visitMethod(access, name, descriptor, signature, exceptions)
        val isAbstractMethod = access and Opcodes.ACC_ABSTRACT != 0
        val isNativeMethod = access and Opcodes.ACC_NATIVE != 0
        if (isAbstractMethod || isInterface || isNativeMethod || methodVisitor == null || "<init>" == name || "<clinit>" == name) {
            return methodVisitor
        }
        methodVisitor = object : AdviceAdapter(api, methodVisitor, access, name, descriptor) {

            /**
             * 扫描类的注解时调用
             * @param descriptor 注解名称
             * @param visible
             * @return
             */
            override fun visitAnnotation(descriptor: String?, visible: Boolean): AnnotationVisitor {
                var annotationVisitor = super.visitAnnotation(descriptor, visible)
                traceMap[descriptor]?.let { trace ->
                    val newTrace = trace.clone()
                    annotationVisitor = object : AnnotationVisitor(api, annotationVisitor) {
                        override fun visit(name: String?, value: Any?) {
                            super.visit(name, value)
                            // 保存注解的参数值
                            name?.let { newTrace.annotationData[it] = value }
                        }

                        override fun visitEnd() {
                            super.visitEnd()
                            newTrace.name = name
                            newTrace.desc = descriptor ?: ""
                            traceMap[newTrace.name + newTrace.desc] = newTrace
                        }
                    }
                }
                return annotationVisitor
            }

            /**
             * 动态方法 (Lambda表达式) 时调用
             * @param name
             * @param descriptor
             * @param bootstrapMethodHandle
             * @param bootstrapMethodArguments
             */
            override fun visitInvokeDynamicInsn(
                name: String?,
                descriptor: String?,
                bootstrapMethodHandle: Handle?,
                vararg bootstrapMethodArguments: Any?
            ) {
                super.visitInvokeDynamicInsn(
                    name, descriptor, bootstrapMethodHandle, *bootstrapMethodArguments
                )
                val desc = bootstrapMethodArguments[0]
                traceMap[name + desc]?.let { trace ->
                    val parent = Type.getReturnType(descriptor).descriptor
                    if (parent == trace.owner) {
                        val handle = bootstrapMethodArguments[1] as Handle
                        val newTrace = trace.clone()
                        newTrace.name = handle.name
                        newTrace.desc = handle.desc
                        traceMap[newTrace.name + newTrace.desc] = newTrace
                    }
                }
            }

            /**
             * 方法进入时调用
             */
            override fun onMethodEnter() {
                super.onMethodEnter()
                traceMap[name + descriptor]?.let { trace ->
                    if (trace.onMethod == 1) {
                        onMethod(trace)
                    }
                }
            }

            /**
             * 方法退出前调用
             */
            override fun onMethodExit(opcode: Int) {
                super.onMethodExit(opcode)
                traceMap[name + descriptor]?.let { trace ->
                    if (trace.onMethod == 0) {
                        onMethod(trace)
                    }
                }
            }

            private fun onMethod(trace: TraceBean) {
                if (trace.annotationDesc.isNotBlank() && trace.annotationData.isNotEmpty()) {
                    // 遍历注解参数并赋值给采集方法
                    trace.annotationParams.forEach { entry ->
                        if (entry.key == "this") {
                            //所在方法的当前对象的引用
                            mv.visitVarInsn(ALOAD, 0)
                        } else {
                            mv.visitLdcInsn(trace.annotationData[entry.key])
                            val type = Type.getType(entry.value)
                            val slotIndex = newLocal(type)
                            mv.visitVarInsn(type.getOpcode(ISTORE), slotIndex)
                            mv.visitVarInsn(type.getOpcode(ILOAD), slotIndex)
                        }
                    }
                    mv.visitMethodInsn(
                        INVOKESTATIC,
                        trace.traceOwner,
                        trace.traceName,
                        trace.traceDesc,
                        false
                    )
                    // 防止其他类重名方法被插入
                    traceMap.remove(name + descriptor, trace)
                } else {
                    // 获取方法参数
                    val methodType = Type.getMethodType(descriptor)
                    val methodArguments = methodType.argumentTypes
                    // 采集数据的方法参数起始索引（ 0：this，1+：普通参数 ），如果是static，则从0开始计算
                    var slotIndex = if (access and ACC_STATIC != 0) 0 else 1
                    // 获取采集方法参数
                    val traceMethodType = Type.getMethodType(trace.traceDesc)
                    val traceArguments = ArrayList(traceMethodType.argumentTypes.asList())
                    // 将扫描方法参数赋值给采集方法
                    methodArguments.forEach { methodArgument ->
                        val size = methodArgument.size
                        val opcode = methodArgument.getOpcode(ILOAD)
                        val desc = methodArgument.descriptor
                        val traceIterator = traceArguments.iterator()
                        // 遍历采集方法参数
                        while (traceIterator.hasNext()) {
                            val traceArgument = traceIterator.next()
                            val traceDesc = traceArgument.descriptor
                            if (traceDesc == desc) {
                                mv.visitVarInsn(opcode, slotIndex)
                                traceIterator.remove()
                                break
                            }
                        }
                        slotIndex += size
                    }
                    // 无法满足采集方法参数则return
                    if (traceArguments.isNotEmpty()) {
                        return
                    }
                    mv.visitMethodInsn(
                        INVOKESTATIC,
                        trace.traceOwner,
                        trace.traceName,
                        trace.traceDesc,
                        false
                    )
                }
            }

        }
        return methodVisitor
    }

}
