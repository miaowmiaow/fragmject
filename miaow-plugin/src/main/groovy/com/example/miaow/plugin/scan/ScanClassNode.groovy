package com.example.fragment.plugin.scan

import com.example.fragment.plugin.MiaowPlugin
import com.example.fragment.plugin.scan.bean.ScanBean
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.tree.*

class ScanClassNode extends ClassNode {

    private ClassVisitor classVisitor;

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
                    MethodNode newMethodNode = new MethodNode(api, methodNode.access, methodNode.name, methodNode.desc, methodNode.signature, methodNode.exceptions.toArray(new String[methodNode.exceptions.size()]))
                    MethodVisitor methodVisitor = new ScanAdviceAdapter(api, newMethodNode, name, methodNode.access, methodNode.name, methodNode.desc)
                    methodNode.accept(methodVisitor)
                    methods.set(i, newMethodNode)
                    println(name + "." + methodNode.name + "->" + methodNode.desc + " \n" + insnNode.owner + "." + insnNode.name + "->" + insnNode.desc + " \n")
                }
            }
        }
        super.visitEnd()
        if (classVisitor != null) {
            accept(classVisitor)
        }
    }

}
