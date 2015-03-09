package com.intendia.qualifier.processor;

import com.intendia.qualifier.processor.ReflectionHelper.QualifyExtensionData;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.MirroredTypeException;

public class SimpleProcessorExtension extends AbstractQualifierProcessorExtension {
    public SimpleProcessorExtension() {
        addAnnotationAnalyzer(Simple.class, new QualifierAnnotationAnalyzer<Simple>() {
            @Override
            public void processAnnotation(AnnotationContext<Simple> ctx) {
                final Simple value = ctx.getAnnotation();
                ctx.getContext().putIfNotNull("simple.loaded", value == null ? null : "xxx");
                ctx.getContext().put("Simple.getString", value.getString());
                ctx.getContext().put("Simple.getInteger", value.getInteger());

                TypeElement typeElement = getProcessingEnv().getElementUtils().getTypeElement("java.lang.Class");
                DeclaredType classType = getProcessingEnv().getTypeUtils().getDeclaredType(typeElement);
                ctx.getContext().put(QualifyExtensionData.of("Simple.getType", classType, parametersType(value)));
            }
        });
    }

    private static String parametersType(Simple annotation) {
        // http://blog.retep.org/2009/02/13/getting-class-values-from-annotations-in-an-annotationprocessor/
        try {
            annotation.getType();
            return null; // this must not happens
        } catch (MirroredTypeException exception) {
            return exception.getTypeMirror().toString();
        }
    }
}
