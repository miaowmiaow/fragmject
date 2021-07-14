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
                newCell.isLambda = true
                newCell.methodName = handle.getName()
                newCell.methodDesc = handle.getDesc()
                StatisticPlugin.HOOKS.put(newCell.methodName + newCell.methodDesc, newCell)
            }
        }
    }

    /**
     * 方法进入时调用
     */
    @Override
    protected void onMethodEnter() {
        super.onMethodEnter()
        BuryPointCell buryPointCell = StatisticPlugin.HOOKS.get(methodName + methodDescriptor)
        if (buryPointCell != null && !buryPointCell.isMethodExit) {
            onMethod(buryPointCell)
        }
    }

    /**
     * 方法退出前调用
     */
    @Override
    protected void onMethodExit(int opcode) {
        BuryPointCell buryPointCell = StatisticPlugin.HOOKS.get(methodName + methodDescriptor)
        if (buryPointCell != null && buryPointCell.isMethodExit) {
            onMethod(buryPointCell)
        }
        super.onMethodExit(opcode)
    }

    private void onMethod(BuryPointCell cell) {
        // 获取方法参数
        Type methodType = Type.getMethodType(methodDescriptor)
        Type[] methodArguments = methodType.getArgumentTypes()
        int methodArgumentSize = methodArguments.size()
        if (cell.isAnnotation) { // 遍历注解参数并赋值给采集方法
            def entrySet = cell.annotationParams.entrySet()
            def size = entrySet.size()
            for (int i = 0; i < size; i++) {
                def key = entrySet[i].getKey()
                if (key == "this") {
                    mv.visitVarInsn(Opcodes.ALOAD, 0)
                } else {
                    def load = entrySet[i].getValue()
                    def store = getVarInsn(load)
                    mv.visitLdcInsn(cell.annotationData.get(key))
                    mv.visitVarInsn(store, i + methodArgumentSize + 1)
                    mv.visitVarInsn(load, i + methodArgumentSize + 1)
                }
            }
            mv.visitMethodInsn(INVOKESTATIC, cell.agentParent, cell.agentName, cell.agentDesc, false)
            // 防止其他类重名方法被插入
            StatisticPlugin.HOOKS.remove(methodName + methodDescriptor, cell)
        } else { // 将扫描方法参数赋值给采集方法
            // 采集数据的方法参数起始索引（ 0：this，1+：普通参数 ），如果是static，则从0开始计算
            int slotIndex = isStatic(methodAccess) ? 0 : 1
            // 获取采集方法参数
            Type agentMethodType = Type.getMethodType(cell.agentDesc)
            Type[] agentArguments = agentMethodType.getArgumentTypes()
            List<Type> agentArgumentList = new ArrayList<Type>(Arrays.asList(agentArguments))
            // 遍历方法参数
            for (Type argument : methodArguments) {
                int size = argument.getSize()
                int opcode = argument.getOpcode(ILOAD)
                String descriptor = argument.getDescriptor()
                Iterator<Type> agentIterator = agentArgumentList.iterator()
                // 遍历采集方法参数
                while (agentIterator.hasNext()) {
                    Type agentArgument = agentIterator.next()
                    String agentDescriptor = agentArgument.getDescriptor()
                    if (agentDescriptor == descriptor) {
                        mv.visitVarInsn(opcode, slotIndex)
                        agentIterator.remove()
                        break
                    }
                }
                slotIndex += size
            }
            if (agentArgumentList.size() > 0) { // 无法满足采集方法参数则return
                return
            }
            mv.visitMethodInsn(INVOKESTATIC, cell.agentParent, cell.agentName, cell.agentDesc, false)
            if(cell.isLambda){
                StatisticPlugin.HOOKS.remove(methodName + methodDescriptor, cell)
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