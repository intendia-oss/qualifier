// Copyright 2013 Intendia, SL.

package com.intendia.qualifier.processor;

import static com.google.common.base.MoreObjects.ToStringHelper;
import static com.google.common.base.Throwables.getStackTraceAsString;
import static com.google.common.collect.FluentIterable.from;
import static java.lang.String.format;
import static java.util.EnumSet.of;
import static javax.lang.model.element.ElementKind.CLASS;
import static javax.lang.model.element.ElementKind.INTERFACE;
import static javax.lang.model.element.Modifier.ABSTRACT;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;
import static javax.tools.Diagnostic.Kind.ERROR;
import static javax.tools.Diagnostic.Kind.NOTE;
import static javax.tools.Diagnostic.Kind.OTHER;
import static javax.tools.Diagnostic.Kind.WARNING;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.MoreObjects;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.intendia.qualifier.annotation.Qualify;
import com.intendia.qualifier.annotation.SkipStaticQualifierMetamodelGenerator;
import com.squareup.javawriter.JavaWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;
import javax.annotation.Generated;
import javax.annotation.Nullable;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;

/** Static Qualifier Metamodel Processor. */
public class StaticQualifierMetamodelProcessor extends AbstractProcessor implements Processor {

    private static final Collection<ElementKind> CLASS_OR_INTERFACE = EnumSet.of(CLASS, INTERFACE);
    private static final Set<String> RESERVED_PROPERTIES = ImmutableSet.of("getName", "getType", "get", "set",
            "comparator", "summary", "abbreviation", "description", "unit", "quantity", "as");
    private static Set<Element> processed = new HashSet<>();

    private DeclaredType declaredType;
    private List<QualifierProcessorExtension> processorExtensions;
    private @Nullable Element element;

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return ImmutableSet.of(Qualify.class.getName());
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment round) {
        try {
            for (String annotation : getSupportedAnnotationTypes()) {
                printMessage("Processing " + annotation + " " + round);
                for (Element element : round.getElementsAnnotatedWith(elementUtils().getTypeElement(annotation))) {
                    final String processingMessage = "Qualifying " + element + " ";
                    this.element = element;
                    try {
                        process(element);
                        printMessage(processingMessage + " [success]");
                    } catch (Exception e) {
                        printError(processingMessage + " [failure]: " + e + "\n" + getStackTraceAsString(e));
                    } finally {
                        this.element = null;
                    }
                }
            }
            return true;
        } catch (Exception e) {
            printError("Fatal error processing " + annotations + round + ": " + e + "\n" + getStackTraceAsString(e));
            throw Throwables.propagate(e);
        }
    }

    public void process(Element proxyElement) {
        if (!CLASS_OR_INTERFACE.contains(proxyElement.getKind())) return;
        if (processed.contains(proxyElement)) return;
        if (proxyElement.getAnnotation(SkipStaticQualifierMetamodelGenerator.class) != null) return;
        processed.add(proxyElement);

        final ReflectionHelper reflection = new ReflectionHelper(
                processingEnv, (TypeElement) proxyElement, getProcessorExtensions());

        // 'Class' refer full qualified name, 'Name' refer to simple class name
        final String beanName = reflection.getSimpleClassName();
        final String qualifyName = reflection.getFlatName() + "__";

        Filer filer = processingEnv.getFiler();
        try (Writer sourceWriter = filer.createSourceFile(qualifyName, proxyElement).openWriter()) {
            final JavaWriter writer = new JavaWriter(sourceWriter);
            final Iterable<? extends QualifierDescriptor> qualifiers = reflection.getPropertyDescriptors();

            ToStringHelper diagnostic = MoreObjects.toStringHelper(qualifyName);

            writer.emitPackage(reflection.getPackageName());

            writer.emitStaticImports(
                    "com.google.gwt.i18n.client.Constants.Generate",
                    "com.google.gwt.i18n.client.Constants.GenerateKeys"
                    );
            writer.emitImports(
                    "com.intendia.qualifier.*",
                    "java.util.Map",
                    "java.util.Set",
                    "com.google.common.collect.ImmutableMap",
                    "com.google.common.collect.Sets",
                    "javax.annotation.Generated",
                    "javax.measure.unit.Unit",
                    "javax.measure.quantity.Quantity",
                    "com.google.gwt.core.shared.GWT",
                    "com.google.gwt.cell.client.Cell",
                    "com.google.gwt.text.shared.Renderer",
                    "com.google.gwt.text.shared.SafeHtmlRenderer"
                    ).emitEmptyLine();

            // TODO proxyClassName == beanName ??
            String proxyClassName = writer.compressType(reflection.getClassRepresenter().asType().toString());
            final String generatedValue = format("\"%s\"", StaticQualifierMetamodelProcessor.class.getName());
            writer.emitAnnotation(SuppressWarnings.class.getSimpleName(), "\"unused\"");
            writer.emitAnnotation(Generated.class.getSimpleName(), generatedValue);
            // public abstract class BeanClass extends BaseQualifier<> implements BeanQualifier<>
            writer.beginType(qualifyName, "class", of(PUBLIC, ABSTRACT),
                    qualifierBase(proxyClassName, proxyClassName), // extends
                    format("BeanQualifier<%s>", proxyClassName)); // implements

            // Private constructor
            writer.beginMethod(null, qualifyName, of(PRIVATE)).endMethod().emitEmptyLine();

            // Bean qualifier extensions
            for (QualifierProcessorExtension extension : getProcessorExtensions()) {
                if (extension.processable()) extension.processBeanQualifier(writer, beanName, qualifiers);
            }

            // Emit qualifiers for each bean property
            for (QualifierDescriptor qualifier : qualifiers) {
                emitQualifier(writer, beanName, qualifyName, diagnostic, proxyClassName, qualifier);
            }

            // All qualifiers instance
            final String qualifiersType = "<String, " + qualifierType(proxyClassName, "?") + ">";
            final String qualifiersFieldName = "qualifiers";

            StringBuilder sb = new StringBuilder();
            for (QualifierDescriptor property : qualifiers) {
                sb.append(format(".put(%1$s.getName(), %1$s)", ReflectionHelper.toLower(property.getName())));
            }

            writer.emitField(format("Map%s", qualifiersType), qualifiersFieldName, of(PUBLIC, STATIC, FINAL),
                    format("ImmutableMap.%sbuilder()%s.build()", qualifiersType, sb.toString()));

            writer.endType();
            printDigest(String.format("Generated static qualifying metamodel %s.", diagnostic.toString()));
        } catch (Exception e) {
            e.printStackTrace();
            printError(format("Fatal error '%s' processing type %s%n%s", e, proxyElement, getStackTraceAsString(e)));
            throw new RuntimeException(e);
        }

    }

    private void emitQualifier(JavaWriter writer, String beanName, String qualifyName, ToStringHelper diagnostic,
            String proxyClassName, QualifierDescriptor property) throws IOException {
        final String propertyName = ReflectionHelper.toLower(property.getName());
        final String propertyType = writer.compressType(property.getType().toString());
        final String propertyClass = beanName + ReflectionHelper.toUpper(propertyName);
        final boolean isSelf = property.getName().equals(ReflectionHelper.SELF);

        diagnostic.add(propertyName, propertyType);

        // Property field and class
        final String type = isSelf
                ? qualifierBean(proxyClassName, propertyType)
                : qualifierType(proxyClassName, propertyType);
        final String extendsType = isSelf ? qualifyName : qualifierBase(proxyClassName, propertyType);

        // public static final Qualifier<Person,Address> address = new PersonAddress();
        writer.emitField(propertyClass, propertyName, of(PUBLIC, STATIC, FINAL), format("new %s()", propertyClass));

        // public final static Person__ PersonMetadata = new PersonSelf();
        final String[] referenceSplit = proxyClassName.split("[.]");
        final String selfReference = referenceSplit[referenceSplit.length - 1] + "Metadata";
        if (isSelf) writer.emitField(propertyClass, selfReference, of(PUBLIC, STATIC, FINAL), "self");

        // public Qualifier<Person, Address> address(){ return PersonMetadata.as(address); }
        if (!isSelf && !RESERVED_PROPERTIES.contains(propertyName)) {
            writer.beginMethod(type, propertyName, of(PUBLIC))
                    .emitStatement(format("return %s.as(%s)", selfReference, propertyName))
                    .endMethod();
        }

        // public static final class PersonAddress extends BaseQualifier<Person,Address> {
        writer.beginType(propertyClass, "class", of(PUBLIC, STATIC, FINAL), extendsType);

        // Property name
        writer.emitAnnotation(Override.class)
                .beginMethod("String", "getName", of(PUBLIC))
                .emitStatement("return \"%s\"", propertyName)
                .endMethod().emitEmptyLine();

        // Property type
        writer.emitAnnotation(Override.class);
        writer.beginMethod(format("Class<%s>", propertyType), "getType", of(PUBLIC));
        if (propertyType.indexOf('<') < 0) writer.emitStatement("return %s.class", propertyType);
        else writer.emitStatement("return (Class) %s.class", propertyType.split("<")[0]);
        writer.endMethod().emitEmptyLine();

        // Property generics
        final List<? extends TypeMirror> typeArguments = property.getType().getTypeArguments();
        if (!typeArguments.isEmpty()) {
            writer.emitAnnotation(Override.class);
            writer.beginMethod("Class<?>[]", "getGenerics", of(PUBLIC));
            writer.emitStatement("return new Class<?>[]{%s}", from(typeArguments)
                    .transform(new Function<TypeMirror, String>() {
                        public String apply(TypeMirror input) {
                            return input.getKind() == TypeKind.WILDCARD ? "null" : input + ".class";
                        }
                    }).join(Joiner.on(',')));
            writer.endMethod().emitEmptyLine();
        }

        // Property getter
        final ExecutableElement getter = property.getGetter();
        if (getter != null) {
            // get()
            writer.emitAnnotation(Override.class);
            if (getter.getAnnotation(Nullable.class) != null) writer.emitAnnotation(Nullable.class);
            writer.beginMethod(propertyType, "get", of(PUBLIC), proxyClassName, "object");
            if (getter.getParameters().size() == 0) {
                writer.emitStatement("return object.%s()", getter.getSimpleName());
            } else {
                String categoryClass = getter.getEnclosingElement().asType().toString();
                writer.emitStatement("return %s.%s(object)", categoryClass, getter.getSimpleName());
            }
            writer.endMethod().emitEmptyLine();
            // isReadable()
            writer.emitAnnotation(Override.class);
            writer.beginMethod("Boolean", "isReadable", of(PUBLIC));
            writer.emitStatement("return Boolean.TRUE");
            writer.endMethod().emitEmptyLine();
        }
        // Property setter
        final ExecutableElement setter = property.getSetter();
        if (setter != null) {
            // set()
            writer.emitAnnotation(Override.class);
            if (setter.getParameters().size() == 1) {
                String parameter = setter.getParameters().get(0).getAnnotation(Nullable.class) != null ?
                        "@javax.annotation.Nullable " + propertyType : propertyType;
                writer.beginMethod("void", "set", of(PUBLIC), proxyClassName, "object", parameter, "value");
                writer.emitStatement("object.%s(value)", setter.getSimpleName());
            } else {
                String parameter = setter.getParameters().get(1).getAnnotation(Nullable.class) != null ?
                        "@javax.annotation.Nullable " + propertyType : propertyType;
                writer.beginMethod("void", "set", of(PUBLIC), proxyClassName, "object", parameter, "value");
                String categoryClass = setter.getEnclosingElement().asType().toString();
                writer.emitStatement("%s.%s(object, value)", categoryClass, setter.getSimpleName());
            }
            writer.endMethod().emitEmptyLine();
            // isWritable()
            writer.emitAnnotation(Override.class);
            writer.beginMethod("Boolean", "isWritable", of(PUBLIC));
            writer.emitStatement("return Boolean.TRUE");
            writer.endMethod().emitEmptyLine();
        }
        // Property self
        if (isSelf) {
            writer.emitAnnotation(Override.class);
            writer.beginMethod(propertyType, "get", of(PUBLIC), proxyClassName, "object");
            writer.emitStatement("return object");
            writer.endMethod().emitEmptyLine();

            // Methods of BeanQualifier

            // public Set<Qualifier<? super QualifiedClass, ?>> getPropertyQualifiers() {
            final String returnType = format("Set<Qualifier<? super %s, ?>>", proxyClassName);
            writer.emitAnnotation(Override.class);
            writer.beginMethod(returnType, "getPropertyQualifiers", of(PUBLIC));
            writer.emitStatement(format("final %s filtered = Sets.newIdentityHashSet()", returnType));
            writer.emitStatement("filtered.addAll(qualifiers.values())");
            writer.emitStatement("filtered.remove(this)");
            writer.emitStatement("return filtered");
            writer.endMethod().emitEmptyLine();
        }

        for (QualifierProcessorExtension extension : getProcessorExtensions()) {
            if (extension.processable()) extension.processPropertyQualifier(writer, beanName, propertyName, property);
        }

        // Property context
        StringBuilder sb = new StringBuilder();
        for (QualifyExtensionData extension : property.getExtensions()) { // add extensions
            sb.append(String.format("\n.put(\"%s\", %s)", extension.getKey(), extension.toLiteral()));
        }

        writer.emitAnnotation(Override.class)
                .beginMethod("Map<String,Object>", "getContext", of(PUBLIC))
                .emitStatement("return ImmutableMap.<String,Object>builder()%s.build()", sb.toString())
                .endMethod();

        writer.endType().emitEmptyLine();
    }

    private synchronized List<QualifierProcessorExtension> getProcessorExtensions() {
        if (processorExtensions == null) {
            printMessage("Loading extensions...");
            final ServiceLoader<QualifierProcessorExtension> loader = ServiceLoader
                    .load(QualifierProcessorExtension.class, getClass().getClassLoader());
            /*
             * This extra-safe loading prevents recursive compilation failures if the processor try to use an not yet
             * compiled extension.
             */
            final ImmutableList.Builder<QualifierProcessorExtension> builder = new ImmutableList.Builder<>();
            final Iterator<QualifierProcessorExtension> iterator = loader.iterator();
            for (;;) {
                try {
                    if (iterator.hasNext()) {
                        final QualifierProcessorExtension next = iterator.next();
                        printMessage("Loaded " + next);
                        builder.add(next);
                    }
                    else break;
                } catch (Throwable exception) {
                    printWarning("Error loading extensions: " + exception);
                }
            }
            final List<QualifierProcessorExtension> extensions = builder.build();
            for (QualifierProcessorExtension extension : extensions) {
                extension.init(processingEnv);
            }
            processorExtensions = extensions;
        }
        return this.processorExtensions;
    }

    private Elements elementUtils() {
        return processingEnv.getElementUtils();
    }

    private String qualifierType(String simpleName, String propertyType) {
        return String.format("Qualifier<%s,%s>", simpleName, propertyType);
    }

    private String qualifierBase(String simpleName, String propertyType) {
        return String.format("BaseQualifier<%s,%s>", simpleName, propertyType);
    }

    private String qualifierBean(String beanType, String propertyType) {
        assert beanType.equals(propertyType) : "bean type " + beanType + " must be equals to property type "
                + propertyType;
        return String.format("BeanQualifier<%s>", beanType);
    }

    public void printDigest(String message) {
        if (Boolean.valueOf(processingEnv.getOptions().get("digest"))) {
            processingEnv.getMessager().printMessage(OTHER, message);
        }
    }

    public void printMessage(String message) {
        processingEnv.getMessager().printMessage(NOTE, message);
    }

    public void printWarning(String message) {
        processingEnv.getMessager().printMessage(WARNING, message, element);
    }

    public void printError(String message) {
        processingEnv.getMessager().printMessage(ERROR, message, element);
    }

}
