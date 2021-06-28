package com.example.plugin.statistic.bp

import org.objectweb.asm.*
import org.objectweb.asm.commons.AdviceAdapter

class BuryPointAnnotationVisitor extends AnnotationVisitor {


    BuryPointAnnotationVisitor(AnnotationVisitor annotationVisitor) {
        super(Opcodes.ASM7, annotationVisitor)
    }

}