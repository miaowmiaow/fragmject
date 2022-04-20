package com.example.miaow.plugin.scan

import com.example.miaow.plugin.MiaowPlugin
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.*

class ScanClassNode extends ClassNode {

    private ClassVisitor classVisitor

    ScanClassNode(int api, ClassVisitor classVisitor) {
        super(api)
        this.classVisitor = classVisitor
    }

    @Override
    void visitEnd() {
        int size = methods.size()
        for (int i = 0; i < size; i++) {
            MethodNode methodNode = methods.get(i)
            if (methodNode.name == "<init>" || methodNode.name == "<clinit>") {
                continue
            }
            InsnList instructions = methodNode.instructions
            if (instructions.size() == 0) {
                continue
            }
            ListIterator<AbstractInsnNode> iterator = instructions.iterator()
            while (iterator.hasNext()) {
                AbstractInsnNode insnNode = iterator.next()
                ScanBean find = null
                if (insnNode instanceof FieldInsnNode) {
                    find = MiaowPlugin.SCAN_FIELDS.find {
                        it.owner == insnNode.owner && it.name == insnNode.name && it.desc == insnNode.desc
                    }
                }
                if (insnNode instanceof MethodInsnNode) {
                    find = MiaowPlugin.SCAN_METHODS.find {
                        it.owner == insnNode.owner && it.name == insnNode.name && it.desc == insnNode.desc
                    }
                }
                if (find != null) {
                    String str = name + "." + methodNode.name + "->" + methodNode.desc + " \n" + insnNode.owner + "." + insnNode.name + "->" + insnNode.desc + " \n"
//                    InsnList il = new InsnList()
//                    il.add(new FieldInsnNode(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;"))
//                    il.add(new LdcInsnNode(str))
//                    il.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false))
//                    instructions.insertBefore(insnNode, il)
                    println(str)
                }
            }
        }
        super.visitEnd()
        if (classVisitor != null) {
            accept(classVisitor)
        }
    }

}
