// Copyright 2014 Intendia, SL.
package com.intendia.qualifier.processor;

import static com.google.common.collect.FluentIterable.from;
import static com.intendia.qualifier.Qualifiers.CORE_NAME;
import static com.intendia.qualifier.Qualifiers.I18N_ABBREVIATION;
import static com.intendia.qualifier.Qualifiers.I18N_DESCRIPTION;
import static com.intendia.qualifier.Qualifiers.I18N_SUMMARY;
import static com.intendia.qualifier.Qualifiers.MEASURE_QUANTITY;
import static com.intendia.qualifier.Qualifiers.MEASURE_UNIT_OF_MEASURE;
import static com.intendia.qualifier.Qualifiers.REPRESENTER_CELL;
import static com.intendia.qualifier.Qualifiers.REPRESENTER_HTML_RENDERER;
import static com.intendia.qualifier.Qualifiers.REPRESENTER_TEXT_RENDERER;
import static com.intendia.qualifier.processor.ReflectionHelper.QualifyExtensionData;

import com.google.common.base.Predicate;
import com.intendia.qualifier.annotation.Qualify;
import com.intendia.qualifier.annotation.QualifyExtension;
import java.util.Collection;
import java.util.List;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.MirroredTypeException;
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
        qCtx.putIfNotNull(REPRESENTER_TEXT_RENDERER, ctx.getAnnotation().renderer());
        qCtx.putIfNotNull(REPRESENTER_HTML_RENDERER, ctx.getAnnotation().safeHtmlRenderer());
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
        context.put(qualifyExtensionData);

        final TypeElement type = getProcessingEnv().getElementUtils()
                .getTypeElement(qualifyExtensionData.getType().toString());

        // Do not requires validation
        if (type.toString().equals("java.lang.String")) return;
        if (type.toString().equals("java.lang.Class")) return;

        // Check extensions types has a valid valueOf(String) static method
        final boolean valid = from(getExecutableElements(type)).anyMatch(new Predicate<ExecutableElement>() {
            @Override
            public boolean apply(ExecutableElement input) {
                final String name = input.getSimpleName().toString();
                if (!name.equals("valueOf")) return false;
                if (!input.getModifiers().contains(Modifier.STATIC)) return false;
                if (input.getParameters().size() != 1) return false;
                if (!isFirstParameterStringType(input)) return false;
                return true;
            }
        });
        if (!valid) {
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
