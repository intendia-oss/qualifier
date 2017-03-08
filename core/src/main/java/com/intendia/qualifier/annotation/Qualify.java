package com.intendia.qualifier.annotation;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
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

    /**
     * A tag interface for the AutoQualifier processor. Generates an extension for each annotation property and
     * a qualifier to access to this metadata.
     */
    @Retention(SOURCE) @Target(ANNOTATION_TYPE) @interface Auto {}

    /**
     * Can be used in {@link Auto} properties of type {@link Class}. In this case, the extension and qualifier will
     * be of type {@link com.intendia.qualifier.Qualifier} instead of type {@link Class}. The {@link Class} should
     * be {@link Qualify}.
     */
    @Retention(SOURCE) @Target(METHOD) @interface Link {}

    /** Used in a {@link Qualify} to extends its metamodel with other metamodel. */
    @Retention(SOURCE) @Target({ TYPE, METHOD, FIELD }) @interface Extend {
        Class<?> value();
        /** (Optional) The referenced property name. Defaults to the same name. */
        String name() default "";
    }
}
