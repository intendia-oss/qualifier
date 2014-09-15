// Copyright 2014 Intendia, SL.
package com.intendia.qualifier.processor;

import java.lang.annotation.Annotation;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;

public class AnnotationContext<A extends Annotation> {
    private final QualifierContext context;
    private final Element annotatedElement;
    private final AnnotationMirror annotationMirror;
    private final A annotation;

    public AnnotationContext(QualifierContext context, Element annotatedElement, AnnotationMirror annotationMirror, A annotation) {
        this.context = context;
        this.annotatedElement = annotatedElement;
        this.annotationMirror = annotationMirror;
        this.annotation = annotation;
    }

    public QualifierContext getContext() {
        return context;
    }

    public Element getAnnotatedElement() {
        return annotatedElement;
    }

    public AnnotationMirror getAnnotationMirror() {
        return annotationMirror;
    }

    public A getAnnotation() {
        return annotation;
    }
}
