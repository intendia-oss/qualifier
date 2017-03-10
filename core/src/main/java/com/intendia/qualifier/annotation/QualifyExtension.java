package com.intendia.qualifier.annotation;

import java.lang.annotation.Target;

/** Allow define qualifier extensions (see Qualifier Extensions). */
@Target({})
public @interface QualifyExtension {

    /** (Required) The qualifier extension key. */
    String key();

    /** (Optional) The qualifier extension type. Only types with static {@code valueOf(String)} allowed. */
    Class<?> type() default String.class;

    /** (Required) The qualifier extension value. */
    String value();

}
