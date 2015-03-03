// Copyright 2014 Intendia, SL.
package com.intendia.qualifier.processor;

import static java.util.EnumSet.of;
import static javax.lang.model.element.Modifier.PUBLIC;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.gwt.inject.rebind.util.Preconditions;
import com.squareup.javawriter.JavaWriter;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.List;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.type.MirroredTypeException;

public abstract class AbstractQualifierProcessorExtension implements QualifierProcessorExtension {
    private ProcessingEnvironment processingEnv;
    private boolean initialized = false;
    private List<TypedQualifierAnnotationAnalyzerDecorator<?>> annotationAnalyzers;

    protected AbstractQualifierProcessorExtension() {
        this.annotationAnalyzers = Lists.newArrayList();
    }

    protected <A extends Annotation> void addAnnotationAnalyzer(Class<A> type, QualifierAnnotationAnalyzer<A> analyzers) {
        this.annotationAnalyzers.add(new TypedQualifierAnnotationAnalyzerDecorator<>(type, analyzers));
    }

    public synchronized void init(ProcessingEnvironment processingEnv) {
        Preconditions.checkState(!initialized, "Cannot call init more than once.");
        this.processingEnv = Preconditions.checkNotNull(processingEnv, "Tool provided null ProcessingEnvironment");
        this.processingEnv = processingEnv;
        this.annotationAnalyzers = ImmutableList.copyOf(annotationAnalyzers);
        this.initialized = true;
    }

    @Override
    public boolean processable() {
        return true;
    }

    @Override
    public Iterable<TypedQualifierAnnotationAnalyzerDecorator<?>> getSupportedAnnotations() {
        return annotationAnalyzers;
    }

    @Override
    public void processBeanQualifier(JavaWriter writer, String beanName,
            Iterable<? extends QualifierDescriptor> properties) throws IOException {}

    @Override
    public void processPropertyQualifier(JavaWriter writer, String beanName, String propertyName,
            QualifierDescriptor property) throws IOException {}

    public void overrideMethod(JavaWriter writer, String returnType, String name, String statementPattern,
            Object... statementArgs) throws IOException {
        final String pattern = statementPattern.startsWith("return ") ? statementPattern : "return " + statementPattern;
        writer.emitAnnotation(Override.class)
                .beginMethod(returnType, name, of(PUBLIC))
                .emitStatement(pattern, statementArgs)
                .endMethod().emitEmptyLine();
    }

    public boolean classExists(String first, String... rest) {
        if (!classExists(first)) return false;
        for (String className : rest) {
            if (!classExists(className)) return false;
        }
        return true;
    }

    public boolean classExists(String className) {
        try {
            return getClass().getClassLoader().loadClass(className) != null;
        } catch (Exception notFound) {
            return false;
        }
    }

    public String classToString(Class<?> type) {
        try {
            return type.toString();
        } catch (MirroredTypeException capture) {
            return capture.getTypeMirror().toString();
        }
    }

    @Override
    public ProcessingEnvironment getProcessingEnv() {
        return processingEnv;
    }

    public static class TypedQualifierAnnotationAnalyzerDecorator<A extends Annotation>
            implements QualifierAnnotationAnalyzer<A> {
        private final Class<A> annotationType;
        private final QualifierAnnotationAnalyzer<A> annotationAnalyzer;

        public TypedQualifierAnnotationAnalyzerDecorator(Class<A> type,
                QualifierAnnotationAnalyzer<A> annotationAnalyzer) {
            this.annotationType = type;
            this.annotationAnalyzer = annotationAnalyzer;
        }

        public Class<A> annotationType() {
            return annotationType;
        }

        @Override
        public void processAnnotation(AnnotationContext<A> annotationContext) {
            annotationAnalyzer.processAnnotation(new AnnotationContext<A>(annotationContext.getContext(), annotationContext.getAnnotatedElement(), annotationContext.getAnnotationMirror(), annotationContext.getAnnotation()));
        }
    }

}
