package com.intendia.qualifier.annotation;

import static java.lang.annotation.RetentionPolicy.SOURCE;

import java.lang.annotation.Retention;

/** Allow define qualifier extensions (see Qualifier Extensions). */
@Retention(SOURCE) public @interface QualifyExtension {

    /** (Required) The qualifier extension key. */
    String key();

    /** (Optional) The qualifier extension type. Only types with static {@code valueOf(String)} allowed. */
    Class<?> type() default String.class;

    /** (Required) The qualifier extension value. */
    String value();

}
