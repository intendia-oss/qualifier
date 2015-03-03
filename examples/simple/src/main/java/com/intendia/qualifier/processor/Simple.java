package com.intendia.qualifier.processor;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Simple {
    String getString();
    Class<?> getType();
    int getInteger();
}
