// Copyright 2014 Intendia, SL.
package com.intendia.qualifier.processor;

import static com.google.common.collect.FluentIterable.from;
import static com.intendia.qualifier.Qualifier.CORE_NAME;

import com.intendia.qualifier.annotation.Qualify;
import com.intendia.qualifier.annotation.QualifyExtension;
import java.util.Collection;
import java.util.List;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic;

public class CoreQualifierProcessor extends AbstractQualifierProcessorExtension {

    public CoreQualifierProcessor() { registerAnnotation(Qualify.class, this::processQualify); }

    private void processQualify(AnnotationContext<Qualify> ctx) {
        final QualifierMetadata qCtx = ctx.getMetadata();
        qCtx.putIfNotNull(CORE_NAME, ctx.getAnnotation().name());

        if (ctx.getAnnotation().extend() != null) {
            for (QualifyExtension qualifyExtension : ctx.getAnnotation().extend()) {
                addQualifyExtension(qCtx, ctx.getAnnotatedElement(), ctx.getAnnotationMirror(), qualifyExtension);
            }
        }
    }

    public void addQualifyExtension(QualifierMetadata context, Element annotatedElement,
            AnnotationMirror annotationMirror, QualifyExtension qualifyExtension) {
        final QualifierMetadata.Entry entry = context.put(qualifyExtension);

        final TypeElement type = getProcessingEnv().getElementUtils()
                .getTypeElement(entry.getType().toString());

        // Do not requires validation
        if (type.toString().equals("java.lang.String")) return;
        if (type.toString().equals("java.lang.Class")) return;

        // Check extensions types has a valid valueOf(String) static method
        final boolean valid = from(getExecutableElements(type)).anyMatch(method ->
                method.getSimpleName().toString().equals("valueOf")
                        && method.getModifiers().contains(Modifier.STATIC)
                        && method.getParameters().size() == 1
                        && isFirstParameterStringType(method));
        if (!valid) {
            getProcessingEnv().getMessager().printMessage(Diagnostic.Kind.ERROR, String.format(
                            "Qualifier extension type '%s' with key '%s' must have a 'valueOf(String)' static " +
                                    "method", entry.getType(), entry.getKey()),
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
