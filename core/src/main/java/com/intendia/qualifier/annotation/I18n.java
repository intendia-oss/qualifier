// Copyright 2014 Intendia, SL.
package com.intendia.qualifier.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD, ElementType.FIELD })
public @interface I18n {
    /** (Optional) The name of the property (e.g. 'User logo'). Defaults to the property name. */
    String summary() default "";

    /** (Optional) The abbreviation or acronym of the property (e.g. 'Logo'). Defaults to the property summary. */
    String abbreviation() default "";

    /** (Optional) The description of the property (e.g. 'The user profile logo.'). Defaults to the property summary. */
    String description() default "";
}
