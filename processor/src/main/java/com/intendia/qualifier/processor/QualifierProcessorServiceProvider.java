package com.intendia.qualifier.processor;

import static java.util.Objects.requireNonNull;
import static javax.lang.model.element.Modifier.PUBLIC;

import com.google.common.base.Preconditions;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

public abstract class QualifierProcessorServiceProvider {
    protected @Nullable ProcessingEnvironment processingEnv;
    private boolean initialized = false;

    synchronized void init(ProcessingEnvironment processingEnv) {
        Preconditions.checkState(!initialized, "cannot call init more than once");
        this.processingEnv = requireNonNull(processingEnv, "tool provided null ProcessingEnvironment");
        this.initialized = true;
    }

    public boolean processable() { return true; }

    public void processAnnotated(Element element, Metaqualifier meta) {}

    /**
     * @param writer static qualifier type spec (ex. some.Person__)
     * @param beanName the simple class name of the qualified bean (ex. Person)
     * @param properties list of properties of Person and itself as self (ex. name, address, self)
     */
    public void processBean(TypeSpec.Builder writer, String beanName, Collection<Metamodel> properties) {}

    /**
     * @param writer property qualifier type spec (ex. some.Person__.PersonName)
     * @param descriptor property metadata access and mutators
     * @deprecated use {@link #processProperty(Metamodel)} instead
     */
    public void processProperty(TypeSpec.Builder writer, Metamodel descriptor) { processProperty(descriptor); }

    public void processProperty(Metamodel descriptor) {}

    // Context

    public final ProcessingEnvironment env() { return requireNonNull(processingEnv, "uninitialized"); }

    public final Types types() { return env().getTypeUtils(); }

    public Elements elements() { return env().getElementUtils(); }

    // Helpers

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

    public static <A extends Annotation> void annotationApply(Element el, Class<A> type, Consumer<A> fn) {
        Optional.ofNullable(el.getAnnotation(type)).ifPresent(fn);
    }
}
