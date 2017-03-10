package com.intendia.qualifier.annotation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.ElementType.TYPE;

import java.lang.annotation.Target;

/** Skip static qualifying metamodel generation on the marked type. */
@Target({ PACKAGE, TYPE, METHOD, FIELD })
public @interface SkipStaticQualifierMetamodelGenerator {
}
