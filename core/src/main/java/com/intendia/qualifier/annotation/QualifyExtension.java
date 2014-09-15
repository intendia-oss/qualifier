package com.intendia.qualifier.annotation;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/** Allow define qualifier extensions (see Qualifier Extensions). */
@Target({ TYPE })
@Retention(RUNTIME)
public @interface QualifyExtension {

    /** (Required) The qualifier extension key. */
    String key();

    /** (Optional) The qualifier extension type. Only types with static {@code valueOf(String)} allowed. */
    Class<?> type() default String.class;

    /** (Required) The qualifier extension value. */
    String value();

}
