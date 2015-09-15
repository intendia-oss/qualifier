package com.intendia.qualifier.processor;

import com.intendia.qualifier.Extension;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Simple {
    Extension<String> STRING = Extension.key("simple.string");
    Extension<Integer> INTEGER = Extension.key("simple.integer");
    Extension<Class<?>> TYPE = Extension.key("simple.type");

    String getString();

    int getInteger();

    Class<?> getType();
}
