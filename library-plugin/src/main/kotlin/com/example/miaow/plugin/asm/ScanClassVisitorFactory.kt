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
import org.objectweb.asm.tree.*

interface ScanParams : InstrumentationParameters {
    @get:Input
    val ignoreOwners: ListProperty<String>

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
        parameters.get().ignoreOwners.get().forEach {
            if (classData.className.startsWith(it.replace("/", "."))) {
                return false
            }
        }
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
                val insnNode = iterator.next()
                if (insnNode is FieldInsnNode) {
                    scans.find {
                        it.owner == insnNode.owner && it.name == insnNode.name && it.desc == insnNode.desc
                    }?.let {
                        instructions.set(insnNode, newInsnNode(it))
                        println(
                            StringBuilder()
                                .append(name).append(".").append(methodNode.name).append("->")
                                .append(methodNode.desc).append(" \n")
                                .append(insnNode.owner).append(".").append(insnNode.name)
                                .append("->")
                                .append(insnNode.desc).append(" \n").toString()
                        )
                    }
                }
                if (insnNode is MethodInsnNode) {
                    scans.find {
                        it.owner == insnNode.owner && it.name == insnNode.name && it.desc == insnNode.desc
                    }?.let {
                        instructions.set(insnNode, newInsnNode(it))
                        println(
                            StringBuilder()
                                .append(name).append(".").append(methodNode.name).append("->")
                                .append(methodNode.desc).append(" \n")
                                .append(insnNode.owner).append(".").append(insnNode.name)
                                .append("->")
                                .append(insnNode.desc).append(" \n").toString()
                        )
                    }
                }
            }
        }
        super.visitEnd()
        accept(classVisitor)
    }

    private fun newInsnNode(bean: ScanBean): AbstractInsnNode {
        val opcode = bean.replaceOpcode
        val owner = bean.replaceOwner
        val name = bean.replaceName
        val descriptor = bean.replaceDesc
        return if (!bean.replaceDesc.startsWith("(")) {
            FieldInsnNode(opcode, owner, name, descriptor)
        } else {
            MethodInsnNode(opcode, owner, name, descriptor, false)
        }
    }

    private fun insertInfo(instructions: InsnList, insnNode: AbstractInsnNode, str: String) {
        val il = InsnList()
        il.add(FieldInsnNode(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;"))
        il.add(LdcInsnNode(str))
        il.add(
            MethodInsnNode(
                Opcodes.INVOKEVIRTUAL,
                "java/io/PrintStream",
                "println",
                "(Ljava/lang/String;)V",
                false
            )
        )
        instructions.insertBefore(insnNode, il)
        println(str)
    }

}
