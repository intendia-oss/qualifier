// Copyright 2014 Intendia, SL.
package com.intendia.qualifier.processor;

import static java.util.Objects.requireNonNull;
import static javax.lang.model.element.Modifier.PUBLIC;

import com.google.auto.common.AnnotationMirrors;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import java.lang.annotation.Annotation;
import java.util.List;
import javax.annotation.Nullable;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

public abstract class AbstractQualifierProcessorExtension implements QualifierProcessorExtension {
    protected @Nullable ProcessingEnvironment processingEnv;
    private boolean initialized = false;
    private List<AnnotationAnalyzerEntry<?>> annotationAnalyzers;

    protected AbstractQualifierProcessorExtension() { this.annotationAnalyzers = Lists.newArrayList(); }

    // Configuration

    protected <A extends Annotation> void registerAnnotation(Class<A> type, QualifierAnnotationAnalyzer<A> analyzer) {
        annotationAnalyzers.add(new MyAnnotationAnalyzerEntry<>(type, analyzer));
    }

    @Override public Iterable<AnnotationAnalyzerEntry<?>> getSupportedAnnotations() { return annotationAnalyzers; }

    // Processing environment

    public synchronized void init(ProcessingEnvironment processingEnv) {
        Preconditions.checkState(!initialized, "cannot call init more than once");
        this.processingEnv = requireNonNull(processingEnv, "tool provided null ProcessingEnvironment");
        this.initialized = true;
    }

    @Override public ProcessingEnvironment getProcessingEnv() { return requireNonNull(processingEnv, "uninitialized"); }

    public Types types() { return getProcessingEnv().getTypeUtils(); }

    public Elements elements() { return getProcessingEnv().getElementUtils(); }

    public TypeElement typeElementFor(Class<?> clazz) { return elements().getTypeElement(clazz.getCanonicalName()); }

    // Helpers

    public MethodSpec override(TypeName returnType, String name, String format, Object... args) {
        format = format.startsWith("return ") ? format : "return " + format;
        return MethodSpec.methodBuilder(name)
                .addAnnotation(Override.class)
                .returns(returnType)
                .addModifiers(PUBLIC)
                .addStatement(format, args)
                .build();
    }

    public boolean classExists(String className) {
        try {
            return getClass().getClassLoader().loadClass(className) != null;
        } catch (Exception notFound) {
            return false;
        }
    }

    private static class MyAnnotationAnalyzerEntry<A extends Annotation> implements AnnotationAnalyzerEntry<A> {
        private final Class<A> annotationType;
        private final QualifierAnnotationAnalyzer<A> annotationAnalyzer;

        public MyAnnotationAnalyzerEntry(Class<A> type, QualifierAnnotationAnalyzer<A> annotationAnalyzer) {
            this.annotationType = type;
            this.annotationAnalyzer = annotationAnalyzer;
        }

        @Override public Class<A> annotationType() { return annotationType; }

        @Override public void process(QualifierMetadata c, Element annotated, AnnotationMirror aMirror, A aClass) {
            annotationAnalyzer.processAnnotation(new MyAnnotationContext<>(c, annotated, aMirror, aClass));
        }
    }

    private static class MyAnnotationContext<A extends Annotation> implements AnnotationContext<A> {
        private final QualifierMetadata context;
        private final Element annotatedElement;
        private final AnnotationMirror annotationMirror;
        private final A annotation;

        public MyAnnotationContext(QualifierMetadata c, Element annotated, AnnotationMirror aMirror, A aClass) {
            this.context = c;
            this.annotatedElement = annotated;
            this.annotationMirror = aMirror;
            this.annotation = aClass;
        }

        @Override public QualifierMetadata getMetadata() { return context; }

        @Override public Element getAnnotatedElement() { return annotatedElement; }

        @Override public AnnotationMirror getAnnotationMirror() { return annotationMirror; }

        @Override public AnnotationValue getAnnotationValue(String elementName) {
            return AnnotationMirrors.getAnnotationValue(annotationMirror, elementName);
        }

        @Override public A getAnnotation() { return annotation; }
    }
}
