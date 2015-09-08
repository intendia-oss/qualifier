package com.intendia.qualifier.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Is used to specify the mapped qualifying for a resource property or field. If no <code>Qualify</code> annotation is
 * specified, the default values apply.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD, ElementType.FIELD })
public @interface Qualify {

    /** (Optional) Override the property name (e.g. 'userLogo'). Defaults to the property name. */
    String name() default "";

    /** (Optional) The type of the property (e.g. Number.class). Defaults to property type. */
    Class<?> type() default Default.class;

    /** (Optional) An array of <code>QualifyExtension</code> annotations. */
    QualifyExtension[] extend() default {};

    class Default {}
}
