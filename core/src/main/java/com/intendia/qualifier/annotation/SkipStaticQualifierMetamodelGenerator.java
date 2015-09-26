package com.intendia.qualifier.annotation;

import static java.lang.annotation.RetentionPolicy.SOURCE;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/** Skip static qualifying metamodel generation on the marked type. */
@Retention(SOURCE) @Target({ ElementType.METHOD, ElementType.PACKAGE, ElementType.TYPE })
public @interface SkipStaticQualifierMetamodelGenerator {
}
