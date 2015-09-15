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

    /** (Optional) An array of <code>QualifyExtension</code> annotations. */
    QualifyExtension[] extend() default {};

}
