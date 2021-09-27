package com.example.plugin.statistic.bp

import org.objectweb.asm.AnnotationVisitor

class BuryPointAnnotationVisitor extends AnnotationVisitor {

    BuryPointAnnotationVisitor(int api, AnnotationVisitor annotationVisitor) {
        super(api, annotationVisitor)
    }

}