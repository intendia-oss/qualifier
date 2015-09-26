// Copyright 2015 Intendia, SL.
package com.intendia.qualifier.example;

import static java.lang.annotation.RetentionPolicy.SOURCE;

import com.intendia.qualifier.annotation.Qualify;
import java.lang.annotation.Retention;

@Qualify.Auto @Retention(SOURCE)
public @interface ExampleAuto {
    String getString();

    int getInteger();

    Class<?> getType();
}
