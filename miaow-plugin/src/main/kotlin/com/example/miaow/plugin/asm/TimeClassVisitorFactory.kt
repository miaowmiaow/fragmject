package com.example.miaow.plugin.asm

import com.android.build.api.instrumentation.AsmClassVisitorFactory
import com.android.build.api.instrumentation.ClassContext
import com.android.build.api.instrumentation.ClassData
import com.android.build.api.instrumentation.InstrumentationParameters
import com.example.miaow.plugin.bean.TimeBean
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Input
import org.objectweb.asm.*
import org.objectweb.asm.commons.AdviceAdapter

interface TimeParams : InstrumentationParameters {
    @get:Input
    val listOfTimes: ListProperty<TimeBean>
}

abstract class TimeClassVisitorFactory : AsmClassVisitorFactory<TimeParams> {

    override fun createClassVisitor(
        classContext: ClassContext,
        nextClassVisitor: ClassVisitor
    ): ClassVisitor {
        return TimeClassVisitor(nextClassVisitor, parameters.get().listOfTimes.get())
    }

    override fun isInstrumentable(classData: ClassData): Boolean {
        val times = parameters.get().listOfTimes.get()
        for (time in times) {
            if (classData.className.startsWith(time.owner.replace("/", "."))) {
                return true
            }
        }
        return false
    }

}

class TimeClassVisitor(
    classVisitor: ClassVisitor,
    val times: List<TimeBean>
) : ClassVisitor(Opcodes.ASM9, classVisitor) {

    private var owner: String = ""
    private var isInterface = false

    /**
     * @param version 类版本
     * @param access 修饰符
     * @param name 类名
     * @param signature 泛型信息
     * @param superName 父类
     * @param interfaces 实现的接口
     */
    override fun visit(
        version: Int,
        access: Int,
        name: String?,
        signature: String?,
        superName: String?,
        interfaces: Array<out String>?
    ) {
        super.visit(version, access, name, signature, superName, interfaces)
        owner = name ?: ""
        isInterface = access and Opcodes.ACC_INTERFACE != 0
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

            var slotIndex = -1

            override fun onMethodEnter() {
                super.onMethodEnter()
                times.forEach {
                    if ((it.time > 0 && owner.contains(it.owner)) || (owner == it.owner && name == it.name && descriptor == it.desc)) {
                        slotIndex = newLocal(Type.LONG_TYPE)
                        mv.visitMethodInsn(
                            INVOKESTATIC,
                            "java/lang/System",
                            "currentTimeMillis",
                            "()J",
                            false
                        )
                        mv.visitVarInsn(LSTORE, slotIndex)
                    }
                }
            }

            override fun onMethodExit(opcode: Int) {
                times.forEach {
                    if ((it.time > 0 && owner.contains(it.owner)) || (owner == it.owner && name == it.name && descriptor == it.desc)) {
                        mv.visitMethodInsn(
                            INVOKESTATIC,
                            "java/lang/System",
                            "currentTimeMillis",
                            "()J",
                            false
                        )
                        mv.visitVarInsn(LLOAD, slotIndex)
                        mv.visitInsn(LSUB)
                        mv.visitVarInsn(LSTORE, slotIndex)
                        mv.visitVarInsn(LLOAD, slotIndex)
                        mv.visitLdcInsn(it.time)
                        mv.visitInsn(LCMP)
                        val label0 = Label()
                        mv.visitJumpInsn(IFLE, label0)
                        mv.visitFieldInsn(
                            GETSTATIC,
                            "java/lang/System",
                            "out",
                            "Ljava/io/PrintStream;"
                        )
                        mv.visitTypeInsn(NEW, "java/lang/StringBuilder")
                        mv.visitInsn(DUP)
                        mv.visitMethodInsn(
                            INVOKESPECIAL,
                            "java/lang/StringBuilder",
                            "<init>",
                            "()V",
                            false
                        )
                        mv.visitLdcInsn("$owner/$name$descriptor --> execution time : (")
                        mv.visitMethodInsn(
                            INVOKEVIRTUAL,
                            "java/lang/StringBuilder",
                            "append",
                            "(Ljava/lang/String;)Ljava/lang/StringBuilder;",
                            false
                        )
                        mv.visitVarInsn(LLOAD, slotIndex)
                        mv.visitMethodInsn(
                            INVOKEVIRTUAL,
                            "java/lang/StringBuilder",
                            "append",
                            "(J)Ljava/lang/StringBuilder;",
                            false
                        )
                        mv.visitLdcInsn("ms)")
                        mv.visitMethodInsn(
                            INVOKEVIRTUAL,
                            "java/lang/StringBuilder",
                            "append",
                            "(Ljava/lang/String;)Ljava/lang/StringBuilder;",
                            false
                        )
                        mv.visitMethodInsn(
                            INVOKEVIRTUAL,
                            "java/lang/StringBuilder",
                            "toString",
                            "()Ljava/lang/String;",
                            false
                        )
                        mv.visitMethodInsn(
                            INVOKEVIRTUAL,
                            "java/io/PrintStream",
                            "println",
                            "(Ljava/lang/String;)V",
                            false
                        )
                        mv.visitLabel(label0)
                    }
                }
                super.onMethodExit(opcode)
            }
        }
        return methodVisitor
    }

}
