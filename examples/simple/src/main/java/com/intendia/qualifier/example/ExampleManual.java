package com.intendia.qualifier.example;

import static java.lang.annotation.RetentionPolicy.SOURCE;

import com.intendia.qualifier.annotation.Qualify;
import java.lang.annotation.Retention;
import java.util.concurrent.TimeUnit;

@Retention(SOURCE)
public @interface ExampleManual {
    String string();

    int integer();

    Class<?> type();

    TimeUnit enumeration();
}
