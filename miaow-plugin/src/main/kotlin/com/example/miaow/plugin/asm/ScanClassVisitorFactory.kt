package com.example.miaow.plugin.asm

import com.android.build.api.instrumentation.AsmClassVisitorFactory
import com.android.build.api.instrumentation.ClassContext
import com.android.build.api.instrumentation.ClassData
import com.android.build.api.instrumentation.InstrumentationParameters
import com.example.miaow.plugin.bean.ScanBean
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Input
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.MethodInsnNode

interface ScanParams : InstrumentationParameters {
    @get:Input
    val listOfScans: ListProperty<ScanBean>
}

abstract class ScanClassVisitorFactory : AsmClassVisitorFactory<ScanParams> {

    override fun createClassVisitor(
        classContext: ClassContext,
        nextClassVisitor: ClassVisitor
    ): ClassVisitor {
        return ScanClassNode(
            nextClassVisitor,
            parameters.get().listOfScans.get(),
        )
    }

    override fun isInstrumentable(classData: ClassData): Boolean {
        return true
    }

}

class ScanClassNode(
    private val classVisitor: ClassVisitor,
    private val scans: List<ScanBean>,
) : ClassNode(Opcodes.ASM9) {

    override fun visitEnd() {
        methods.forEach { methodNode ->
            val instructions = methodNode.instructions
            val iterator = instructions.iterator()
            while (iterator.hasNext()) {
                val insnNode: AbstractInsnNode = iterator.next()
                if (insnNode is FieldInsnNode) {
                    scans.find {
                        !it.isMethod && it.owner == insnNode.owner && it.name == insnNode.name && it.desc == insnNode.desc
                    }?.let {
                        println(name + "." + methodNode.name + "->" + methodNode.desc + " \n" + insnNode.owner + "." + insnNode.name + "->" + insnNode.desc + " \n")
                    }
                }
                if (insnNode is MethodInsnNode) {
                    scans.find {
                        it.isMethod && it.owner == insnNode.owner && it.name == insnNode.name && it.desc == insnNode.desc
                    }?.let {
                        println(name + "." + methodNode.name + "->" + methodNode.desc + " \n" + insnNode.owner + "." + insnNode.name + "->" + insnNode.desc + " \n")
                    }
                }
            }
        }
        super.visitEnd()
        accept(classVisitor)
    }

}
