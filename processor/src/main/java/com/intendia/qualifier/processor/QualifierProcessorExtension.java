// Copyright 2014 Intendia, SL.
package com.intendia.qualifier.processor;

import static com.intendia.qualifier.processor.AbstractQualifierProcessorExtension.TypedQualifierAnnotationAnalyzerDecorator;

import com.squareup.javawriter.JavaWriter;
import java.io.IOException;
import javax.annotation.processing.ProcessingEnvironment;

public interface QualifierProcessorExtension {

    void init(ProcessingEnvironment processingEnv);

    boolean processable();

    Iterable<TypedQualifierAnnotationAnalyzerDecorator<?>> getSupportedAnnotations();

    void processBeanQualifier(JavaWriter writer, String beanName, Iterable<? extends QualifierDescriptor> qualifiers)
            throws IOException;

    void processPropertyQualifier(JavaWriter writer, String beanName, String propertyName, QualifierDescriptor property)
            throws IOException;

    ProcessingEnvironment getProcessingEnv();
}
