// Copyright 2014 Intendia, SL.
package com.intendia.qualifier.processor;

import java.lang.annotation.Annotation;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;

public interface QualifierAnnotationAnalyzer<A extends Annotation> {
    void processAnnotation(AnnotationContext<A> annotationContext);
}
