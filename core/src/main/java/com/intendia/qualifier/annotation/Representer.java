// Copyright 2014 Intendia, SL.
package com.intendia.qualifier.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD, ElementType.FIELD })
public @interface Representer {

    /** (Optional) The renderer of the property. Defaults to best effort renderer. */
    String textRenderer() default "";

    /** (Optional) The safe html renderer. Defaults to html scape of {@link #textRenderer()}. */
    String htmlRenderer() default "";

    /** (Optional) The cell of the property. Defaults to wrapper cell of {@link #htmlRenderer()}. */
    String cell() default "";

}
