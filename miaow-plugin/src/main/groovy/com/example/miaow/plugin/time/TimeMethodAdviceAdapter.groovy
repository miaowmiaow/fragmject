package com.example.fragment.plugin.time

import com.example.fragment.plugin.MiaowPlugin
import com.example.fragment.plugin.time.bean.TimeMethodBean
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Type
import org.objectweb.asm.commons.AdviceAdapter

class TimeMethodAdviceAdapter extends AdviceAdapter {

    String methodOwner
    String methodName
    int slotIndex

    TimeMethodAdviceAdapter(int api, MethodVisitor methodVisitor, String owner, int access, String name, String desc) {
        super(api, methodVisitor, access, name, desc)
        this.methodOwner = owner
        this.methodName = name
    }

    @Override
    protected void onMethodEnter() {
        super.onMethodEnter()
        for (TimeMethodBean bean : MiaowPlugin.METHOD_TIMER_LIST) {
            if (methodOwner.contains(bean.getOwner())) {
                slotIndex = newLocal(Type.LONG_TYPE)
                mv.visitMethodInsn(INVOKESTATIC, "java/lang/System", "currentTimeMillis", "()J", false)
                mv.visitVarInsn(LSTORE, slotIndex)
            }
        }
    }

    @Override
    void onMethodExit(int opcode) {
        for (TimeMethodBean bean : MiaowPlugin.METHOD_TIMER_LIST) {
            if (methodOwner.contains(bean.getOwner())) {
                mv.visitMethodInsn(INVOKESTATIC, "java/lang/System", "currentTimeMillis", "()J", false)
                mv.visitVarInsn(LLOAD, slotIndex)
                mv.visitInsn(LSUB)
                mv.visitVarInsn(LSTORE, slotIndex)
                mv.visitVarInsn(LLOAD, slotIndex)
                mv.visitLdcInsn(new Long(bean.getTime()))
                mv.visitInsn(LCMP)
                Label label0 = new Label()
                mv.visitJumpInsn(IFLE, label0)
                mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;")
                mv.visitTypeInsn(NEW, "java/lang/StringBuilder")
                mv.visitInsn(DUP)
                mv.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V", false)
                mv.visitLdcInsn(methodOwner + "/" + methodName + " --> execution time : (")
                mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false)
                mv.visitVarInsn(LLOAD, slotIndex)
                mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(J)Ljava/lang/StringBuilder;", false)
                mv.visitLdcInsn("ms)")
                mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false)
                mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false)
                mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false)
                mv.visitLabel(label0)
            }
        }
        super.onMethodExit(opcode)
    }

}
