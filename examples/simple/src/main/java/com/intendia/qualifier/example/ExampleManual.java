package com.intendia.qualifier.example;

import static java.lang.annotation.RetentionPolicy.SOURCE;

import java.lang.annotation.Retention;

@Retention(SOURCE)
public @interface ExampleManual {
    String getString();

    int getInteger();

    Class<?> getType();
}
