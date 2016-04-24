package com.intendia.qualifier.annotation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.SOURCE;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/** Skip static qualifying metamodel generation on the marked type. */
@Retention(SOURCE) @Target({ PACKAGE, TYPE, METHOD, FIELD })
public @interface SkipStaticQualifierMetamodelGenerator {
}
