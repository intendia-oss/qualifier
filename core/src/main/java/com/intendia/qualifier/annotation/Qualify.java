package com.intendia.qualifier.annotation;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;

import java.lang.annotation.Target;

/**
 * Is used to specify the mapped qualifying for a resource property or field. If no <code>Qualify</code> annotation is
 * specified, the default values apply.
 */
@Target({ TYPE, METHOD, FIELD })
public @interface Qualify {

    /** (Optional) Set to true to include fields as qualified properties. */
    boolean fields() default false;

    /** (Optional) An array of manual extensions. Use with caution, better use a specific annotation. */
    Entry[] extend() default {};

    /** (Optional) Apply this extend to all properties without explicit extension definition. */
    Class<?> mixin() default Object.class;

    /** Allow define qualifier extensions (see Qualifier Extensions). */
    @Target({}) @interface Entry {

        /** (Required) The qualifier extension key. */
        String key();

        /** (Optional) The qualifier extension type. Only types with static {@code valueOf(String)} allowed. */
        Class<?> type() default String.class;

        /** (Required) The qualifier extension value. */
        String value();
    }

    /**
     * A tag interface for the AutoQualifier processor. Generates an extension for each annotation property and
     * a qualifier to access to this metadata.
     */
    @Target({ ANNOTATION_TYPE }) @interface Auto {}

    /**
     * Can be used in {@link Auto} properties of type {@link Class}. In this case, the extension and qualifier will
     * be of type {@link com.intendia.qualifier.Qualifier} instead of type {@link Class}. The {@link Class} should
     * be {@link Qualify}.
     */
    @Target({ METHOD }) @interface Link {}

    /** Used in a {@link Qualify}'ed type to extends with <i>other</i> metamodel. */
    @Target({ TYPE, METHOD, FIELD }) @interface Extend {

        /** (Required) The <i>other</i> metamodel type. */
        Class<?> value() default Object.class;

        /** (Optional) The referenced property name. Defaults to the same name. */
        String name() default "";
    }

    /** Skip static qualifying metamodel generation on the marked type. */
    @Target({ TYPE, METHOD, FIELD }) @interface Skip {}
}
