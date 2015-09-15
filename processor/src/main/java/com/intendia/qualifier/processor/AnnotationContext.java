// Copyright 2015 Intendia, SL.
package com.intendia.qualifier.processor;

import java.lang.annotation.Annotation;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;

public interface AnnotationContext<A extends Annotation> {

    A annotation();

    AnnotationMirror annotationMirror();

    AnnotationValue annotationValue(String elementName);

    Element annotatedElement();

    QualifierMetadata metadata();

}
