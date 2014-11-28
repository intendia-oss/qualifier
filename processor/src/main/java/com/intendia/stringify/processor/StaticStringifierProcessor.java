// Copyright 2013 Intendia, SL.

package com.intendia.stringify.processor;

import static com.google.common.base.MoreObjects.ToStringHelper;
import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Strings.emptyToNull;
import static java.lang.String.format;
import static java.util.EnumSet.of;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;
import static javax.tools.Diagnostic.Kind.ERROR;
import static javax.tools.Diagnostic.Kind.NOTE;
import static javax.tools.Diagnostic.Kind.WARNING;

import com.google.common.base.CaseFormat;
import com.google.common.base.Joiner;
import com.squareup.javawriter.JavaWriter;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;

/** Static Qualifier Metamodel Processor. */
public class StaticStringifierProcessor extends AbstractProcessor {

    private static Set<Element> processed = new HashSet<>();

    private ProcessingEnvironment environment;

    public StaticStringifierProcessor() {}

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Collections.singleton(Stringify.class.getName());
    }

    @Override
    public synchronized void init(ProcessingEnvironment environment) {
        super.init(environment);
        this.environment = environment;
    }

    public ProcessingEnvironment getEnvironment() {
        return environment;
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnvironment) {
        if (!roundEnvironment.processingOver()) {
            printMessage(getClass().getName() + " started.");
            for (String supportedAnnotationName : getSupportedAnnotationTypes()) {
                printMessage("Searching for " + supportedAnnotationName + " annotations.");
                try {
                    Class<?> supportedAnnotationClass = Class.forName(supportedAnnotationName);
                    if (supportedAnnotationClass.isAnnotation()) {
                        for (Element annotatedElement : roundEnvironment
                                .getElementsAnnotatedWith((Class<? extends Annotation>) supportedAnnotationClass)) {
                            printMessage("Found " + annotatedElement.toString() + ".");
                            this.process(annotatedElement);
                        }
                    }
                } catch (ClassNotFoundException e) {
                    printError("Annotation not found: " + supportedAnnotationName);
                }
            }
            printMessage(getClass().getName() + " finished.");
        }
        return true;
    }

    public Collection<VariableElement> getFields(TypeElement classRepresenter) {
        List<? extends Element> members = elementUtils().getAllMembers(classRepresenter);
        return ElementFilter.fieldsIn(members);
    }

    public String getPackageName(TypeElement classRepresenter) {
        return elementUtils().getPackageOf(classRepresenter).getQualifiedName().toString();
    }

    /** Returns the class name. (ex. {@code com.gwtplatform.dispatch.shared.annotation.Foo}) */
    public String getClassName(TypeElement classRepresenter) {
        return Joiner.on('.').skipNulls()
                .join(emptyToNull(getPackageName(classRepresenter)), getSimpleClassName(classRepresenter));
    }

    private String getSimpleClassName(TypeElement classRepresenter) {
        return classRepresenter.getSimpleName().toString();
    }

    public void process(Element annotatedElement) {
        if (ElementKind.ENUM != annotatedElement.getKind()) return;
        if (processed.contains(annotatedElement)) return;
        processed.add(annotatedElement);

        // 'Class' refer full qualified name, 'Name' refer to simple class name
        final TypeElement classRepresenter = (TypeElement) annotatedElement;
        final String qualifyName = getClassName(classRepresenter) + "__";

        Filer filer = getEnvironment().getFiler();
        try (Writer sourceWriter = filer.createSourceFile(qualifyName, annotatedElement).openWriter()) {
            final JavaWriter writer = new JavaWriter(sourceWriter);
            Collection<VariableElement> fields = getFields(classRepresenter);

            ToStringHelper diagnostic = toStringHelper(qualifyName);

            writer.emitPackage(getPackageName(classRepresenter));

            writer.beginType(qualifyName, "class", of(PUBLIC, FINAL));

            // Bean qualifier extensions
            for (VariableElement field : fields) {
                final String variableName = field.getSimpleName().toString();
                final String variableValue = CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.LOWER_HYPHEN, variableName);
                writer.emitField("String", variableName, of(PUBLIC, STATIC, FINAL), format("\"%s\"", variableValue));
            }

            writer.endType();
            printMessage(String.format("Generated static stringified type %s.", diagnostic.toString()));
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
            printError(format("Fatal error '%s' processing type %s", e.getMessage(), annotatedElement));
            throw new RuntimeException(e);
        }

    }

    private Elements elementUtils() {
        return getEnvironment().getElementUtils();
    }

    public void printMessage(String message) {
        getEnvironment().getMessager().printMessage(NOTE, message);
    }

    public void printWarning(String message) {
        getEnvironment().getMessager().printMessage(WARNING, message);
    }

    public void printError(String message) {
        getEnvironment().getMessager().printMessage(ERROR, message);
    }

}
