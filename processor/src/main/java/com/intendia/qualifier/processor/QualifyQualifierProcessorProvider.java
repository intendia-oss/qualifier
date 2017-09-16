package com.intendia.qualifier.processor;

import static com.intendia.qualifier.Qualifier.COMPARABLE_COMPARATOR;
import static com.intendia.qualifier.Qualifier.CORE_GENERICS;
import static com.intendia.qualifier.Qualifier.CORE_NAME;
import static com.intendia.qualifier.Qualifier.CORE_TYPE;
import static java.util.stream.Collectors.joining;

import com.google.auto.common.MoreElements;
import com.intendia.qualifier.Qualifier;
import com.intendia.qualifier.annotation.Qualify;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.WildcardTypeName;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic;

public class QualifyQualifierProcessorProvider extends QualifierProcessorServiceProvider {
    public static final WildcardTypeName WILDCARD = WildcardTypeName.subtypeOf(TypeName.OBJECT);
    public static final ClassName LANG_CLASS = ClassName.get(Class.class);

    public QualifyQualifierProcessorProvider() { registerAnnotation(Qualify.class, this::processQualify); }

    private void processQualify(QualifierAnnotationAnalyzer.AnnotationContext<Qualify> ctx) {
        for (Qualify.Entry qualifyExtension : ctx.annotation().extend()) {
            addQualifyExtension(ctx.metadata(), ctx.annotatedElement(), ctx.annotationMirror(), qualifyExtension);
        }
    }

    public void addQualifyExtension(Metaqualifier context, Element annotatedElement,
            AnnotationMirror annotationMirror, Qualify.Entry qualifyExtension) {
        final Metaextension<?> metaextension = context.use(qualifyExtension);

        final TypeMirror type = metaextension.type().get();
        final TypeElement element = MoreElements.asType(types().asElement(type));

        // Do not requires validation
        if (type.toString().equals("java.lang.String")) return;
        if (type.toString().equals("java.lang.Class")) return;

        // Check extensions types has a valid valueOf(String) static method
        final boolean valid = getExecutableElements(element).stream().anyMatch(method ->
                method.getSimpleName().toString().equals("valueOf")
                        && method.getModifiers().contains(Modifier.STATIC)
                        && method.getParameters().size() == 1
                        && isFirstParameterStringType(method));
        if (!valid) {
            getProcessingEnv().getMessager().printMessage(Diagnostic.Kind.ERROR, String.format(
                    "Qualifier extension type '%s' with key '%s' must have a 'valueOf(String)' static " +
                            "method", metaextension.type(), metaextension.extension()),
                    annotatedElement, annotationMirror);
        }

    }

    public boolean isFirstParameterStringType(ExecutableElement input) {
        return input.getParameters().get(0).asType().toString().equals("java.lang.String");
    }

    private Collection<ExecutableElement> getExecutableElements(TypeElement classRepresenter) {
        return ElementFilter.methodsIn(getProcessingEnv().getElementUtils().getAllMembers(classRepresenter));
    }

    @Override public void processProperty(TypeSpec.Builder writer, Metamodel ctx) {
        // Property name
        ctx.metadata().literal(CORE_NAME, "$S", ctx.name());

        TypeMirror propertyType = ctx.propertyType();
        List<? extends TypeMirror> generics = propertyType instanceof DeclaredType
                ? ((DeclaredType) propertyType).getTypeArguments() : Collections.emptyList();

        // Property type
        ctx.metadata().literal(CORE_TYPE, "$L$T.class",
                generics.isEmpty() ? "" : "(Class) ",
                TypeName.get(types().erasure(ctx.propertyType())));

        // Property generics
        if (!generics.isEmpty()) {
            ctx.metadata().literal(CORE_GENERICS, "new Class<?>[]{$L}", generics.stream()
                    .map(t -> t.getKind() == TypeKind.WILDCARD ? "null" : t + ".class")
                    .collect(joining(",")));
        }

        // Property type comparator
        final TypeMirror comparableType = types().erasure(typeElementFor(Comparable.class).asType());
        final boolean isComparable = types().isAssignable(ctx.propertyType(), comparableType);
        if (isComparable) ctx.metadata().literal(COMPARABLE_COMPARATOR, "$T.naturalComparator()", Qualifier.class);
    }
}
