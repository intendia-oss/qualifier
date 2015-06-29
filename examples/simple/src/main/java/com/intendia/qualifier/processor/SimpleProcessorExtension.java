package com.intendia.qualifier.processor;

import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.MirroredTypeException;

public class SimpleProcessorExtension extends AbstractQualifierProcessorExtension {
    public SimpleProcessorExtension() {
        addAnnotationAnalyzer(Simple.class, new QualifierAnnotationAnalyzer<Simple>() {
            @Override
            public void processAnnotation(AnnotationContext<Simple> annotationCtx) {
                final QualifierContext qualifierCtx = annotationCtx.getContext();
                final Simple value = annotationCtx.getAnnotation();
                qualifierCtx.putIfNotNull("simple.loaded", "xxx");
                qualifierCtx.put("simple.getString", value.getString());
                qualifierCtx.put("simple.getInteger", value.getInteger());

                TypeElement typeElement = getProcessingEnv().getElementUtils().getTypeElement("java.lang.Class");
                DeclaredType classType = getProcessingEnv().getTypeUtils().getDeclaredType(typeElement);
                qualifierCtx.putClass("simple.getType", classType, parametersType(value));

                // literal values outputs as a literal expression but has no processor-time value
                QualifyExtensionData literalExtension = qualifierCtx.putLiteral("simple.getLiteral", "new Object()");
                // String processorTimeValue = literalExtension.getValue(String.class); this throws exception
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
