// Copyright 2014 Intendia, SL.
package com.intendia.qualifier.processor;

import com.squareup.javapoet.TypeSpec;
import java.util.Collection;
import javax.annotation.processing.ProcessingEnvironment;

public interface QualifierProcessorExtension {

    default boolean processable() { return true; }

    void init(ProcessingEnvironment processingEnv);

    ProcessingEnvironment getProcessingEnv();

    Iterable<AnnotationAnalyzerEntry<?>> getSupportedAnnotations();

    /**
     * @param writer     static qualifier type spec (ex. some.Person__)
     * @param beanName   the simple class name of the qualified bean (ex. Person)
     * @param properties list of properties of Person and itself as self (ex. name, address, self)
     */
    default void processBean(TypeSpec.Builder writer, String beanName, Collection<PropertyDescriptor> properties) {}

    /**
     * @param writer     property qualifier type spec (ex. some.Person__.PersonName)
     * @param descriptor property metadata access and mutators
     */
    default void processProperty(TypeSpec.Builder writer, PropertyDescriptor descriptor) {}
}
