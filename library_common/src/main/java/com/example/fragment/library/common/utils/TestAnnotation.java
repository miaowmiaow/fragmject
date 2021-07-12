package com.example.fragment.library.common.utils;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * 测试注解
 */
@Target(value = {ElementType.TYPE,ElementType.METHOD})
public @interface TestAnnotation {
    int code() default 0;
    String message() default "";
}
