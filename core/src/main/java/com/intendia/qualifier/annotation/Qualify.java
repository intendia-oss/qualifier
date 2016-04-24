package com.intendia.qualifier.annotation;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static java.lang.annotation.RetentionPolicy.SOURCE;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Is used to specify the mapped qualifying for a resource property or field. If no <code>Qualify</code> annotation is
 * specified, the default values apply.
 */
@Retention(SOURCE) @Target({ TYPE, METHOD, FIELD })
public @interface Qualify {

    /** (Optional) Set to true to include fields as qualified properties. */
    boolean fields() default false;

    /** (Optional) An array of <code>QualifyExtension</code> annotations. */
    QualifyExtension[] extend() default {};

    @Retention(RUNTIME) @Target(ANNOTATION_TYPE) @interface Auto {}

    @Retention(RUNTIME) @Target(METHOD) @interface Link {}
}
