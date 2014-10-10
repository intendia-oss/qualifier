// Copyright 2014 Intendia, SL.
package com.intendia.qualifier.processor;

import static com.intendia.qualifier.Qualifiers.*;
import static com.intendia.qualifier.processor.ReflectionHelper.QualifyExtensionData;

import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.intendia.qualifier.annotation.Qualify;
import com.intendia.qualifier.annotation.QualifyExtension;
import com.intendia.qualifier.extension.RendererExtension;
import java.util.Collection;
import java.util.List;
import javax.lang.model.element.*;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic;

public class CoreQualifierProcessor extends AbstractQualifierProcessorExtension
        implements QualifierAnnotationAnalyzer<Qualify> {

    public CoreQualifierProcessor() {
        addAnnotationAnalyzer(Qualify.class, this);
    }

    @Override
    public void processAnnotation(AnnotationContext<Qualify> ctx) {
        final QualifierContext qCtx = ctx.getContext();
        qCtx.putIfNotNull(CORE_NAME, ctx.getAnnotation().name());

        if (ctx.getAnnotation().extend() != null) {
            for (QualifyExtension qualifyExtension : ctx.getAnnotation().extend()) {
                addQualifyExtension(qCtx, ctx.getAnnotatedElement(), ctx.getAnnotationMirror(), qualifyExtension);
            }
        }

        // I18n
        qCtx.putIfNotNull(I18N_SUMMARY, ctx.getAnnotation().summary());
        qCtx.putIfNotNull(I18N_DESCRIPTION, ctx.getAnnotation().description());
        qCtx.putIfNotNull(I18N_ABBREVIATION, ctx.getAnnotation().abbreviation());
        // Gwt representer
        qCtx.putIfNotNull(RendererExtension.TEXT_RENDERER, ctx.getAnnotation().renderer());
        qCtx.putIfNotNull(RendererExtension.HTML_RENDERER, ctx.getAnnotation().safeHtmlRenderer());
        qCtx.putIfNotNull(REPRESENTER_CELL, ctx.getAnnotation().cell());
        // Measure
        qCtx.putIfNotNull(MEASURE_UNIT_OF_MEASURE, ctx.getAnnotation().unitOfMeasure());
        qCtx.putIfNotNull(MEASURE_QUANTITY, quantityType(ctx.getAnnotation()));
    }

    private String quantityType(Qualify annotation) {
        // http://blog.retep.org/2009/02/13/getting-class-values-from-annotations-in-an-annotationprocessor/
        try {
            annotation.quantity();
            return null; // this must not happens
        } catch (MirroredTypeException exception) {
            return exception.getTypeMirror().toString();
        }
    }

    public void addQualifyExtension(QualifierContext context, Element annotatedElement,
            AnnotationMirror annotationMirror, QualifyExtension qualifyExtension) {
        final QualifyExtensionData qualifyExtensionData = QualifyExtensionData.of(qualifyExtension);
        context.putIfNotNull(qualifyExtension.key(), qualifyExtensionData);

        final TypeElement type = getProcessingEnv().getElementUtils()
                .getTypeElement(qualifyExtensionData.getType().toString());

        // Do not requires validation
        if (type.toString().equals("java.lang.String")) return;
        if (type.toString().equals("java.lang.Class")) return;

        // Check extensions types has a valid valueOf(String) static method
        final TypeElement stringType = getProcessingEnv().getElementUtils().getTypeElement(
                String.class.toString());
        final ImmutableList<ExecutableElement> invalidTypes = FluentIterable
                .from(getExecutableElements(type)).filter(new Predicate<ExecutableElement>() {
                    @Override
                    public boolean apply(ExecutableElement input) {
                        final String name = input.getSimpleName().toString();
                        final TypeMirror returnType = input.getReturnType();

                        if (!name.equals("valueOf")) return false;
                        if (!input.getModifiers().contains(Modifier.STATIC)) return false;
                        if (input.getParameters().size() != 1) return false;
                        if (!isFirstParameterStringType(input)) return false;
                        return true;
                    }
                }).toList();
        if (invalidTypes.isEmpty()) { // non empty implies static valueOf(String) found
            getProcessingEnv().getMessager().printMessage(Diagnostic.Kind.ERROR, String.format(
                    "Qualifier extension type '%s' with key '%s' must have a 'valueOf(String)' static " +
                            "method", qualifyExtensionData.getType(), qualifyExtensionData.getKey()),
                    annotatedElement, annotationMirror);
        }

    }

    public boolean isFirstParameterStringType(ExecutableElement input) {
        return input.getParameters().get(0).asType().toString().equals("java.lang.String");
    }

    private Collection<ExecutableElement> getExecutableElements(TypeElement classRepresenter) {
        List<? extends Element> members = getProcessingEnv().getElementUtils().getAllMembers(classRepresenter);
        return ElementFilter.methodsIn(members);
    }
}
