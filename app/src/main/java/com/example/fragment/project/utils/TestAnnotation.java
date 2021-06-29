package com.example.fragment.project.utils;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@Target(value = {ElementType.TYPE,ElementType.METHOD})
public @interface TestAnnotation {
    String message() default "";
    boolean sb() default false;
}
