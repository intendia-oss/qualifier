package com.intendia.qualifier.processor;

public class SimpleProcessorExtension extends AbstractQualifierProcessorExtension {
    public SimpleProcessorExtension() {
        addAnnotationAnalyzer(Simple.class, new QualifierAnnotationAnalyzer<Simple>() {
            @Override
            public void processAnnotation(AnnotationContext<Simple> ctx) {
                final Simple value = ctx.getAnnotation();
                ctx.getContext().putIfNotNull("simple.loaded", value == null ? null : "xxx");
            }
        });
    }
}
