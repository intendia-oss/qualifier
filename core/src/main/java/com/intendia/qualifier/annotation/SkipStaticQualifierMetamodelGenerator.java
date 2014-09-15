package com.intendia.qualifier.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Skip static qualifying metamodel generation on the marked type. */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.PACKAGE, ElementType.TYPE })
public @interface SkipStaticQualifierMetamodelGenerator {
}
