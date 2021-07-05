package com.example.plugin.statistic.bp

import com.example.plugin.statistic.StatisticPlugin
import org.objectweb.asm.*
import org.objectweb.asm.commons.AdviceAdapter

class BuryPointMethodVisitor extends AdviceAdapter {

    int methodAccess
    String methodName
    String methodDescriptor

    BuryPointMethodVisitor(MethodVisitor methodVisitor, int access, String name, String desc) {
        super(Opcodes.ASM7, methodVisitor, access, name, desc)
        this.methodAccess = access
        this.methodName = name
        this.methodDescriptor = desc
    }

    /**
     * 扫描类的注解时调用
     * @param descriptor 注解名称
     * @param visible
     * @return
     */
    @Override
    AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
        AnnotationVisitor annotationVisitor = super.visitAnnotation(descriptor, visible)
        // 通过descriptor判断是否是需要扫描的注解
        BuryPointCell cell = StatisticPlugin.HOOKS.get(descriptor)
        if (cell != null) {
            BuryPointCell newCell = cell.clone()
            return new BuryPointAnnotationVisitor(annotationVisitor) {
                @Override
                void visit(String name, Object value) {
                    super.visit(name, value)
                    // 保存注解的参数值
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

    /**
     * lambda表达式时调用
     * @param name
     * @param descriptor
     * @param bootstrapMethodHandle
     * @param bootstrapMethodArguments
     */
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

    /**
     * 进入方法时调用
     */
    @Override
    protected void onMethodEnter() {
        super.onMethodEnter()
        BuryPointCell cell = StatisticPlugin.HOOKS.get(methodName + methodDescriptor)
        if (cell != null) {
            Type methodType = Type.getMethodType(methodDescriptor)
            Type[] argumentTypes = methodType.getArgumentTypes() // 获取方法参数
            int methodArgumentSize = argumentTypes.size()
            if (cell.isAnnotation) { // 遍历注解参数并赋值给采集方法
                def entrySet = cell.annotationParams.entrySet()
                def size = entrySet.size()
                for (int i = 0; i < size; i++) {
                    def load = entrySet[i].getValue()
                    def store = getVarInsn(load)
                    mv.visitLdcInsn(cell.annotationData.get(entrySet[i].getKey()))
                    mv.visitVarInsn(store, i + methodArgumentSize + 1)
                    mv.visitVarInsn(load, i + methodArgumentSize + 1)
                }
                mv.visitMethodInsn(INVOKESTATIC, cell.agentParent, cell.agentName, cell.agentDesc, false)
            } else { // 将扫描方法参数赋值给采集方法
                int slotIndex = isStatic(methodAccess) ? 0 : 1
                // 采集数据的方法参数起始索引（ 0：this，1+：普通参数 ），如果是static，则从0开始计算
                Type agentMethodType = Type.getMethodType(cell.agentDesc)
                Type[] agentArgumentTypes = agentMethodType.getArgumentTypes() // 获取采集方法参数
                for (Type argumentType : argumentTypes) { // 技术有限就先这么实现了，希望有更好的方案可以沟通哈
                    int size = argumentType.getSize()
                    int opcode = argumentType.getOpcode(ILOAD)
                    String descriptor = argumentType.getDescriptor()
                    List<Type> agentArgumentTypeList = new ArrayList<Type>(Arrays.asList(agentArgumentTypes))
                    Iterator<Type> agentIterator = agentArgumentTypeList.iterator()
                    while (agentIterator.hasNext()) {
                        Type agentType = agentIterator.next()
                        String agentDescriptor = agentType.getDescriptor()
                        if (agentDescriptor == descriptor) {
                            mv.visitVarInsn(opcode, slotIndex)
                            agentIterator.remove()
                        }
                        break
                    }
                    slotIndex += size
                }
                mv.visitMethodInsn(INVOKESTATIC, cell.agentParent, cell.agentName, cell.agentDesc, false)
            }
        }
    }

    /**
     * 推断类型
     * int ILOAD = 21; int ISTORE = 54;
     * 33 = ISTORE - ILOAD
     *
     * @param load
     * @returno
     */
    private static int getVarInsn(int load) {
        return load + 33
    }

    private static boolean isStatic(int access) {
        return (access & Opcodes.ACC_STATIC) != 0
    }

}