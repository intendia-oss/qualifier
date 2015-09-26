// Copyright 2014 Intendia, SL.
package com.intendia.qualifier.processor;

import java.lang.annotation.Annotation;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;

@FunctionalInterface
public interface QualifierAnnotationAnalyzer<A extends Annotation> {
    void processAnnotation(AnnotationContext<A> annotationContext);

    interface AnnotationContext<A extends Annotation> {
        A annotation();

        AnnotationMirror annotationMirror();

        AnnotationValue annotationValue(String elementName);

        Element annotatedElement();

        Metaqualifier metadata();
    }
}
