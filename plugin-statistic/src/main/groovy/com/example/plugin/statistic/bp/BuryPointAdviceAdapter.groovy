package com.example.plugin.statistic.bp

import com.example.plugin.statistic.StatisticPlugin
import org.objectweb.asm.AnnotationVisitor
import org.objectweb.asm.Handle
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Type
import org.objectweb.asm.commons.AdviceAdapter

class BuryPointAdviceAdapter extends AdviceAdapter {

    int api
    int methodAccess
    String methodName
    String methodDesc

    BuryPointAdviceAdapter(int api, MethodVisitor methodVisitor, int access, String name, String desc) {
        super(api, methodVisitor, access, name, desc)
        this.api = api
        this.methodAccess = access
        this.methodName = name
        this.methodDesc = desc
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
        BuryPointEntity entity = StatisticPlugin.BURY_POINT_MAP.get(descriptor)
        if (entity != null) {
            BuryPointEntity newEntity = entity.clone()
            return new BuryPointAnnotationVisitor(api, annotationVisitor) {
                @Override
                void visit(String name, Object value) {
                    super.visit(name, value)
                    // 保存注解的参数值
                    newEntity.annotationData.put(name, value)
                }

                @Override
                void visitEnd() {
                    super.visitEnd()
                    newEntity.methodName = methodName
                    newEntity.methodDesc = methodDesc
                    StatisticPlugin.BURY_POINT_MAP.put(newEntity.methodName + newEntity.methodDesc, newEntity)
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
        BuryPointEntity entity = StatisticPlugin.BURY_POINT_MAP.get(name + desc)
        if (entity != null) {
            String parent = Type.getReturnType(descriptor).getDescriptor()
            if (parent == entity.methodOwner) {
                Handle handle = (Handle) bootstrapMethodArguments[1]
                BuryPointEntity newEntity = entity.clone()
                newEntity.isLambda = true
                newEntity.methodName = handle.getName()
                newEntity.methodDesc = handle.getDesc()
                StatisticPlugin.BURY_POINT_MAP.put(newEntity.methodName + newEntity.methodDesc, newEntity)
            }
        }
    }

    /**
     * 方法进入时调用
     */
    @Override
    protected void onMethodEnter() {
        super.onMethodEnter()
        BuryPointEntity entity = StatisticPlugin.BURY_POINT_MAP.get(methodName + methodDesc)
        if (entity != null && !entity.isMethodExit) {
            onMethod(entity)
        }
    }

    /**
     * 方法退出前调用
     */
    @Override
    protected void onMethodExit(int opcode) {
        BuryPointEntity entity = StatisticPlugin.BURY_POINT_MAP.get(methodName + methodDesc)
        if (entity != null && entity.isMethodExit) {
            onMethod(entity)
        }
        super.onMethodExit(opcode)
    }

    private void onMethod(BuryPointEntity entity) {
        if (entity.isAnnotation) {
            // 遍历注解参数并赋值给采集方法
            for (Map.Entry<String, String> entry : entity.annotationParams.entrySet()) {
                String key = entry.getKey()
                if (key == "this") {
                    //所在方法的当前对象的引用
                    mv.visitVarInsn(ALOAD, 0)
                } else {
                    mv.visitLdcInsn(entity.annotationData.get(key))
                    Type type = Type.getType(entry.getValue())
                    int slotIndex = newLocal(type)
                    mv.visitVarInsn(type.getOpcode(ISTORE), slotIndex)
                    mv.visitVarInsn(type.getOpcode(ILOAD), slotIndex)
                }
            }
            mv.visitMethodInsn(INVOKESTATIC, entity.agentOwner, entity.agentName, entity.agentDesc, false)
            // 防止其他类重名方法被插入
            StatisticPlugin.BURY_POINT_MAP.remove(methodName + methodDesc, entity)
        } else {
            // 获取方法参数
            Type methodType = Type.getMethodType(methodDesc)
            Type[] methodArguments = methodType.getArgumentTypes()
            // 采集数据的方法参数起始索引（ 0：this，1+：普通参数 ），如果是static，则从0开始计算
            int slotIndex = (methodAccess & ACC_STATIC) != 0 ? 0 : 1
            // 获取采集方法参数
            Type agentMethodType = Type.getMethodType(entity.agentDesc)
            Type[] agentArguments = agentMethodType.getArgumentTypes()
            List<Type> agentArgumentList = new ArrayList<Type>(Arrays.asList(agentArguments))
            // 将扫描方法参数赋值给采集方法
            for (Type methodArgument : methodArguments) {
                int size = methodArgument.getSize()
                int opcode = methodArgument.getOpcode(ILOAD)
                String descriptor = methodArgument.getDescriptor()
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
            mv.visitMethodInsn(INVOKESTATIC, entity.agentOwner, entity.agentName, entity.agentDesc, false)
            if (entity.isLambda) {
                StatisticPlugin.BURY_POINT_MAP.remove(methodName + methodDesc, entity)
            }
        }
    }

}