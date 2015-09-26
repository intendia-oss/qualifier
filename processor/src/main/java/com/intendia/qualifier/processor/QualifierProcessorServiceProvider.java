// Copyright 2014 Intendia, SL.
package com.intendia.qualifier.processor;

import static java.util.Objects.requireNonNull;
import static javax.lang.model.element.Modifier.PUBLIC;

import com.google.auto.common.AnnotationMirrors;
import com.google.auto.common.MoreElements;
import com.google.common.base.Preconditions;
import com.intendia.qualifier.processor.QualifierAnnotationAnalyzer.AnnotationContext;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nullable;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

public abstract class QualifierProcessorServiceProvider {
    protected @Nullable ProcessingEnvironment processingEnv;
    private boolean initialized = false;
    private List<QualifierAnnotationAnalyzerEntry<?>> annotationAnalyzers = new ArrayList<>();

    protected <A extends Annotation> void registerAnnotation(Class<A> type, QualifierAnnotationAnalyzer<A> analyzer) {
        annotationAnalyzers.add(new QualifierAnnotationAnalyzerEntry<>(type, analyzer));
    }

    synchronized void init(ProcessingEnvironment processingEnv) {
        Preconditions.checkState(!initialized, "cannot call init more than once");
        this.processingEnv = requireNonNull(processingEnv, "tool provided null ProcessingEnvironment");
        this.initialized = true;
    }

    public boolean processable() { return true; }

    public void processMethod(Element method, Metaqualifier metaqualifier) {
        annotationAnalyzers.forEach(p -> p.process(method, metaqualifier));
    }

    /**
     * @param writer     static qualifier type spec (ex. some.Person__)
     * @param beanName   the simple class name of the qualified bean (ex. Person)
     * @param properties list of properties of Person and itself as self (ex. name, address, self)
     */
    public void processBean(TypeSpec.Builder writer, String beanName, Collection<Metamodel> properties) {}

    /**
     * @param writer     property qualifier type spec (ex. some.Person__.PersonName)
     * @param descriptor property metadata access and mutators
     */
    public void processProperty(TypeSpec.Builder writer, Metamodel descriptor) {}

    // Helpers

    public ProcessingEnvironment getProcessingEnv() { return requireNonNull(processingEnv, "uninitialized"); }

    public Types types() { return getProcessingEnv().getTypeUtils(); }

    public Elements elements() { return getProcessingEnv().getElementUtils(); }

    public TypeElement typeElementFor(Class<?> clazz) { return elements().getTypeElement(clazz.getCanonicalName()); }

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

    private static class QualifierAnnotationAnalyzerEntry<A extends Annotation> implements AnnotationContext<A> {
        private final Class<A> annotationType;
        private final QualifierAnnotationAnalyzer<A> annotationAnalyzer;

        public QualifierAnnotationAnalyzerEntry(Class<A> type, QualifierAnnotationAnalyzer<A> annotationAnalyzer) {
            this.annotationType = type;
            this.annotationAnalyzer = annotationAnalyzer;
        }

        public void process(Metaqualifier c, Element annotated, AnnotationMirror aMirror, A aClass) {
            annotationAnalyzer.processAnnotation(viewAs(c, annotated, aMirror, aClass));
        }

        public void process(Element annotatedElement, Metaqualifier metaqualifier) {
            A aClass = annotatedElement.getAnnotation(annotationType);
            AnnotationMirror aMirror = MoreElements.getAnnotationMirror(annotatedElement, annotationType).orNull();
            if (aClass != null && aMirror != null) process(metaqualifier, annotatedElement, aMirror, aClass);
        }

        // AnnotationContext view
        private Metaqualifier context;
        private Element annotated;
        private AnnotationMirror aMirror;
        private A aClass;

        public AnnotationContext<A> viewAs(Metaqualifier c, Element e, AnnotationMirror aMirror, A aClass) {
            this.context = c; this.annotated = e; this.aMirror = aMirror; this.aClass = aClass; return this;
        }

        @Override public Metaqualifier metadata() { return context; }

        @Override public Element annotatedElement() { return annotated; }

        @Override public AnnotationMirror annotationMirror() { return aMirror; }

        @Override public A annotation() { return aClass; }

        @Override public AnnotationValue annotationValue(String elementName) {
            return AnnotationMirrors.getAnnotationValue(aMirror, elementName);
        }
    }
}
