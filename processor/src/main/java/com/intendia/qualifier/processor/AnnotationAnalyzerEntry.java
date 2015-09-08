// Copyright 2015 Intendia, SL.
package com.intendia.qualifier.processor;

import java.lang.annotation.Annotation;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;

public interface AnnotationAnalyzerEntry<A extends Annotation> {
    Class<A> annotationType();

    void process(QualifierMetadata context, Element annotatedElement, AnnotationMirror aMirror, A aClass);
}
