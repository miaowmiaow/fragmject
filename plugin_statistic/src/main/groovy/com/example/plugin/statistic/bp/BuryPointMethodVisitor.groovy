package com.example.plugin.statistic.bp

import com.example.plugin.statistic.StatisticPlugin
import org.objectweb.asm.*
import org.objectweb.asm.commons.AdviceAdapter

class BuryPointMethodVisitor extends AdviceAdapter {

    String methodName
    String methodDescriptor

    BuryPointMethodVisitor(MethodVisitor methodVisitor, int access, String name, String desc) {
        super(Opcodes.ASM7, methodVisitor, access, name, desc)
        this.methodName = name
        this.methodDescriptor = desc
    }

    @Override
    AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
        AnnotationVisitor annotationVisitor = super.visitAnnotation(descriptor, visible)
        BuryPointCell cell = StatisticPlugin.HOOKS.get(descriptor)
        if (cell != null) {
            BuryPointCell newCell = cell.clone()
            return new BuryPointAnnotationVisitor(annotationVisitor) {
                @Override
                void visit(String name, Object value) {
                    super.visit(name, value)
                    newCell.annotationData.put(name, value)
                }

                @Override
                void visitEnd() {
                    super.visitEnd()
                    newCell.methodName = methodName
                    newCell.methodDesc = methodDescriptor
                    StatisticPlugin.HOOKS.put(newCell.methodName + newCell.methodDesc, newCell)
                }
            }
        }
        return annotationVisitor
    }

    @Override
    void visitInvokeDynamicInsn(String name, String descriptor, Handle bootstrapMethodHandle, Object... bootstrapMethodArguments) {
        super.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments)
        String desc = (String) bootstrapMethodArguments[0]
        BuryPointCell cell = StatisticPlugin.HOOKS.get(name + desc)
        if (cell != null) {
            String parent = Type.getReturnType(descriptor).getDescriptor()
            if (parent == cell.methodParent) {
                Handle handle = (Handle) bootstrapMethodArguments[1]
                BuryPointCell newCell = cell.clone()
                newCell.methodName = handle.getName()
                newCell.methodDesc = handle.getDesc()
                StatisticPlugin.HOOKS.put(newCell.methodName + newCell.methodDesc, newCell)
            }
        }
    }

    @Override
    protected void onMethodEnter() {
        super.onMethodEnter()
        BuryPointCell cell = StatisticPlugin.HOOKS.get(methodName + methodDescriptor)
        if (cell != null) {
            if (cell.isAnnotation) {
                def entrySet = cell.annotationParams.entrySet()
                def size = entrySet.size()
                for (int i = 0; i < size; i++) {
                    def load = entrySet[i].getValue()
                    def store = getVarInsn(load)
                    mv.visitLdcInsn(cell.annotationData.get(entrySet[i].getKey()))
                    mv.visitVarInsn(store, i + 10)
                    mv.visitVarInsn(load, i + 10)
                }
                mv.visitMethodInsn(INVOKESTATIC, cell.agentParent, cell.agentName, cell.agentDesc, false)
            } else {
                for (int key : cell.methodParams.keySet()) {
                    mv.visitVarInsn(cell.methodParams.get(key), key)
                }
                mv.visitMethodInsn(INVOKESTATIC, cell.agentParent, cell.agentName, cell.agentDesc, false)
            }
        }
    }

    /**
     * 推断类型
     * @param load
     * @returno
     */
    private static int getVarInsn(int load) {
        return load + 33
    }

}