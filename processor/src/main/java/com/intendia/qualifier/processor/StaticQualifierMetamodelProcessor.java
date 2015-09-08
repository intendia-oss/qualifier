// Copyright 2013 Intendia, SL.
package com.intendia.qualifier.processor;

import static com.google.common.base.MoreObjects.ToStringHelper;
import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Throwables.getCausalChain;
import static com.google.common.base.Throwables.getStackTraceAsString;
import static com.intendia.qualifier.Qualifier.CORE_NAME;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;
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

import com.google.auto.common.MoreElements;
import com.google.auto.common.MoreTypes;
import com.google.common.base.CaseFormat;
import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.intendia.qualifier.BaseQualifier;
import com.intendia.qualifier.BeanQualifier;
import com.intendia.qualifier.Qualifier;
import com.intendia.qualifier.annotation.Qualify;
import com.intendia.qualifier.annotation.QualifyExtension;
import com.intendia.qualifier.annotation.SkipStaticQualifierMetamodelGenerator;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.WildcardTypeName;
import java.lang.annotation.Annotation;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.annotation.Generated;
import javax.annotation.Nullable;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

/**
 * Static Qualifier Metamodel Processor.
 * <pre>
 *     model: the actual Pojo model, eg. Person
 *     qualify: the annotations over the model
 *     qualify extension: dynamic qualify annotations
 *     qualifier: the classes to access qualify
 *     metamodel: the qualifier is the metamodel of the model
 *     metadata: the qualifier contains metadatas, metadatas are the runtime representation of the quialify models
 * </pre>
 */
public class StaticQualifierMetamodelProcessor extends AbstractProcessor implements Processor {

    /** Represents an unmodifiable set of lowercase reserved words in Java. */
    public static final Set<String> RESERVED_JAVA_KEYWORDS = ImmutableSet.copyOf(Splitter.on(',').split("abstract," +
            "assert,boolean,break,byte,case,catch,char,class,const,continue,default,do,double,else,enum,extends," +
            "false,final,finally,float,for,goto,if,implements,import,instanceof,int,interface,long,native,new,null," +
            "package,private,protected,public,return,short,static,strictfp,super,switch,synchronized,this,throw," +
            "throws,transient,true,try,void,volatile,while"));
    private static final Collection<ElementKind> CLASS_OR_INTERFACE = EnumSet.of(CLASS, INTERFACE);
    private static final Set<String> RESERVED_PROPERTIES = ImmutableSet.of("getName", "getType", "get", "set",
            "comparator", "summary", "abbreviation", "description", "unit", "quantity", "as");
    public static final WildcardTypeName WILDCARD = WildcardTypeName.subtypeOf(TypeName.OBJECT);
    public static final ClassName LANG_CLASS = ClassName.get(Class.class);
    public static final ClassName LANG_STRING = ClassName.get(String.class);
    private static Set<Element> processed = new HashSet<>();

    private @Nullable List<QualifierProcessorExtension> processorExtensions;
    private @Nullable TypeElement beanElement;

    @Override public SourceVersion getSupportedSourceVersion() { return SourceVersion.latestSupported(); }

    @Override public Set<String> getSupportedAnnotationTypes() { return ImmutableSet.of(Qualify.class.getName()); }

    @Override public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment round) {
        printMessage("Processing " + Qualify.class + " " + toStringHelper(round)
                .add("errorRaised", round.errorRaised()).add("processingOver", round.processingOver()));
        for (Element element : round.getElementsAnnotatedWith(Qualify.class)) {
            // Skip conditions
            if (!CLASS_OR_INTERFACE.contains(element.getKind())) {
                printMessage("ignored " + element + ", cause: is not class or interface");
                continue;
            }
            if (processed.contains(element)) {
                printMessage("ignored " + element + ", cause: already processed");
                continue;
            }
            if (element.getAnnotation(SkipStaticQualifierMetamodelGenerator.class) != null) {
                printMessage("ignored " + element + ", cause: marked with @SkipStaticQualifierMetamodelGenerator");
                continue;
            }

            // Prepare processing environment
            this.beanElement = (TypeElement) element;
            try {

                process(); // now!

                processed.add(beanElement);
                printMessage(format("Qualifying %s [success]", element));
            } catch (Exception e) {
                final String msg = "Qualifying " + element + " " + " [failure]: " + e + "\n" + getStackTraceAsString(e);
                processingEnv.getMessager().printMessage(ERROR, msg, element);
                printError(format("Qualifying %s [failure]: %s\n%s", element,
                        getCausalChain(e).stream().map(Throwable::getLocalizedMessage).collect(joining(", caused by ")),
                        getStackTraceAsString(e)));
            } finally {
                this.beanElement = null;
            }
        }
        return true;
    }

    public void process() throws Exception {
        final TypeHelper beanHelper = new TypeHelper(processingEnv, requireNonNull(beanElement));

        // 'Class' refer full qualified name, 'Name' refer to simple class name
        final String beanName = beanHelper.getSimpleClassName();
        final ClassName beanClassName = ClassName.get(beanElement);
        final ClassName metamodelName = ClassName.bestGuess(beanHelper.getFlatName() + "__");

        // final JavaWriter writer = new JavaWriter(sourceWriter);
        final Collection<PropertyDescriptor> qualifiers = getPropertyDescriptors(beanHelper);

        ToStringHelper diagnostic = MoreObjects.toStringHelper(metamodelName.simpleName());

        // public abstract class BeanClass extends BaseQualifier<> implements BeanQualifier<>
        final TypeSpec.Builder container = TypeSpec.classBuilder(metamodelName.simpleName())
                .addOriginatingElement(beanElement)
                .addModifiers(PUBLIC, ABSTRACT)
                .superclass(qualifierBase(beanClassName, beanClassName))
                .addSuperinterface(ParameterizedTypeName.get(ClassName.get(BeanQualifier.class), beanClassName))
                .addAnnotation(AnnotationSpec.builder(SuppressWarnings.class)
                        .addMember("value", "$S", "unused")
                        .build())
                .addAnnotation(AnnotationSpec.builder(Generated.class)
                        .addMember("value", "$S", ClassName.get(StaticQualifierMetamodelProcessor.class))
                        .addMember("date", "$S", Instant.now())
                        .addMember("comments", "$S", "Enabled extensions (" + getProcessorExtensions().size() + "):\n"
                                + getProcessorExtensions().stream()
                                .map(e -> e.getClass().getSimpleName())
                                .collect(joining("\n")))
                        .build());

        // Private constructor
        container.addMethod(MethodSpec.constructorBuilder().addModifiers(PRIVATE).build());

        // Bean qualifier extensions
        getProcessorExtensions().stream()
                .filter(QualifierProcessorExtension::processable)
                .forEach(extension -> extension.processBean(container, beanName, qualifiers));

        // Emit qualifiers for each bean property
        for (PropertyDescriptor qualifier : qualifiers) {
            emitQualifier(container, diagnostic, metamodelName, qualifier);
        }

        // All qualifiers instance
        {
            final TypeName valueType = qualifierType(beanClassName, WILDCARD);
            final TypeName keyType = LANG_STRING;
            final TypeName mapType = ParameterizedTypeName.get(ClassName.get(Map.class), keyType, valueType);
            container.addField(FieldSpec.builder(mapType, "qualifiers", PUBLIC, STATIC, FINAL)
                    .initializer("$[$T\n.<$T,$T>builder()\n$L\n.build()$]", ImmutableMap.class, keyType, valueType,
                            qualifiers.stream().map(p -> format(".put(%1$s.getName(), %1$s)", toLower(p.getName())))
                                    .collect(joining("\n")))
                    .build());
        }

        printDigest(format("Generated static qualifying metamodel %s.", diagnostic.toString()));

        JavaFile.builder(beanHelper.getPackageName(), container.build()).build().writeTo(processingEnv.getFiler());
    }

    private void emitQualifier(TypeSpec.Builder writer, ToStringHelper diagnostic, ClassName metamodelName,
            PropertyDescriptor descriptor) {

        // ex. Person
        final TypeName beanType = TypeName.get(descriptor.getBeanElement().asType());
        final ClassName beanClassName = ClassName.get(descriptor.getBeanElement());

        // ex. ref: person.address, name: address, type: Address
        final String propertyName = toLower(descriptor.getName());
        final TypeName propertyType = TypeName.get(descriptor.getPropertyType());
        final TypeName propertyRawType = TypeName.get(types().erasure(descriptor.getPropertyType()));

        // the qualifier representing the property, ex. PersonAddress
        final String qualifierClass = beanClassName.simpleName() + toUpper(propertyName);
        final TypeName qualifierClassName = ClassName.get(beanClassName.packageName(), qualifierClass);

        diagnostic.add(propertyName, propertyType);

        // Property field and class

        final DeclaredType extendsType = types().getDeclaredType(typeElementFor(BaseQualifier.class),
                descriptor.getBeanElement().asType(),
                descriptor.getPropertyType());

        // public static final PersonAddress address = new PersonAddress();
        writer.addField(FieldSpec.builder(qualifierClassName, propertyName, PUBLIC, STATIC, FINAL)
                .initializer("new $T()", qualifierClassName)
                .build());

        // public final static PersonSelf PersonMetadata = self;
        final String selfReference = PropertyDescriptor.SELF;
        if (descriptor.isBean()) {
            final String metadata = beanClassName.simpleName() + "Metadata";
            writer.addField(FieldSpec.builder(qualifierClassName, metadata, PUBLIC, STATIC, FINAL)
                    .initializer(PropertyDescriptor.SELF)
                    .build());
        }

        // public PersonAddress address(){ return self.as(address); }
        if (!descriptor.isBean() && !RESERVED_PROPERTIES.contains(propertyName)) {
            writer.addMethod(MethodSpec.methodBuilder(propertyName)
                    .addModifiers(PUBLIC, STATIC)
                    .returns(qualifierType(beanClassName, propertyType))
                    .addStatement("return $N.as($N)", selfReference, propertyName)
                    .build());
        }

        // public static final class PersonAddress extends BaseQualifier<Person,Address> {
        final TypeSpec.Builder property = TypeSpec.classBuilder(qualifierClass)
                .addModifiers(PUBLIC, STATIC, FINAL)
                .superclass(descriptor.isBean() ? metamodelName : TypeName.get(extendsType));

        // Property name
        property.addMethod(MethodSpec.methodBuilder("getName")
                .addModifiers(PUBLIC)
                .returns(LANG_STRING)
                .addStatement("return $S", propertyName)
                .build());

        // Property type
        property.addMethod(MethodSpec.methodBuilder("getType")
                .addModifiers(PUBLIC)
                .returns(ParameterizedTypeName.get(LANG_CLASS, propertyType))
                .addStatement("return (Class) $T.class", propertyRawType)
                .build());

        // Property generics
        final List<? extends TypeMirror> typeArguments = descriptor.getPropertyType().getTypeArguments();
        if (!typeArguments.isEmpty()) {
            property.addMethod(MethodSpec.methodBuilder("getGenerics")
                    .addModifiers(PUBLIC)
                    .returns(ArrayTypeName.of(ParameterizedTypeName.get(LANG_CLASS, WILDCARD)))
                    .addStatement("return new Class<?>[]{$L}", typeArguments.stream()
                            .map(t -> t.getKind() == TypeKind.WILDCARD ? "null" : t + ".class")
                            .collect(joining(",")))
                    .build());
        }

        // Property getter
        final ExecutableElement getter = descriptor.getGetterElement();
        if (getter != null) {
            // get()
            final MethodSpec.Builder getMethod = MethodSpec.methodBuilder("get")
                    .addModifiers(PUBLIC)
                    .returns(propertyType)
                    .addParameter(beanType, "object");
            if (getter.getParameters().isEmpty()) {
                getMethod.addStatement("return object.$N()", getter.getSimpleName());
            } else {
                final TypeName categoryName = ClassName.get(getter.getEnclosingElement().asType());
                getMethod.addStatement("return $T.$N(object)", categoryName, getter.getSimpleName());

            }
            property.addMethod(getMethod.build());

            // isReadable()
            property.addMethod(MethodSpec.methodBuilder("isReadable")
                    .addModifiers(PUBLIC)
                    .returns(TypeName.BOOLEAN.box())
                    .addStatement("return true")
                    .build());
        }
        // Property setter
        final ExecutableElement setter = descriptor.getSetterElement();
        if (setter != null) {
            // set()
            final MethodSpec.Builder getMethod = MethodSpec.methodBuilder("set")
                    .addModifiers(PUBLIC)
                    .addParameter(beanType, "object")
                    .addParameter(propertyType, "value");
            if (setter.getParameters().size() == 1) {
                getMethod.addStatement("object.$N(value)", setter.getSimpleName());
            } else {
                final TypeName categoryName = ClassName.get(setter.getEnclosingElement().asType());
                getMethod.addStatement("$T.$N(object, value)", categoryName, setter.getSimpleName());

            }
            property.addMethod(getMethod.build());

            // isWritable()
            property.addMethod(MethodSpec.methodBuilder("isWritable")
                    .addModifiers(PUBLIC)
                    .returns(TypeName.BOOLEAN.box())
                    .addStatement("return true")
                    .build());
        }

        // Property self
        if (descriptor.isBean()) {
            // get()
            property.addMethod(MethodSpec.methodBuilder("get")
                    .addModifiers(PUBLIC)
                    .returns(propertyType)
                    .addParameter(beanType, "object")
                    .addStatement("return object")
                    .build());

            // Methods of BeanQualifier

            // public Set<Qualifier<? super QualifiedClass, ?>> getPropertyQualifiers() {
            final ParameterizedTypeName propertySetType = ParameterizedTypeName.get(ClassName.get(Set.class),
                    qualifierType(WildcardTypeName.supertypeOf(propertyType), WILDCARD));
            property.addMethod(MethodSpec.methodBuilder("getPropertyQualifiers")
                    .addModifiers(PUBLIC)
                    .returns(propertySetType)
                    .addStatement("final $T filtered = $T.newIdentityHashSet()",
                            propertySetType, ClassName.get(Sets.class))
                    .addStatement("filtered.addAll(qualifiers.values())")
                    .addStatement("filtered.remove(this)")
                    .addStatement("return filtered")
                    .build());
        }

        getProcessorExtensions().stream()
                .filter(QualifierProcessorExtension::processable)
                .forEach(extension -> {
                    try {
                        extension.processProperty(property, descriptor);
                    } catch (Throwable e) {
                        printError(format("Processor %s fail processing property %s.%s, cause: %s\n%s",
                                extension.getClass().getSimpleName(), beanClassName.simpleName(), propertyName,
                                getCausalChain(e).stream()
                                        .map(Throwable::getLocalizedMessage)
                                        .collect(joining(", caused by ")),
                                getStackTraceAsString(e)));
                    }
                });

        // Property context
        StringBuilder sb = new StringBuilder();
        for (QualifierMetadata.Entry extension : descriptor.getExtensions()) { // add extensions
            sb.append(format("\n.put(\"%s\", %s)", extension.getKey(), extension.toLiteral()));
        }

        property.addMethod(MethodSpec.methodBuilder("getContext")
                .addModifiers(PUBLIC)
                .returns(ParameterizedTypeName
                        .get(ClassName.get(Map.class), LANG_STRING, TypeName.OBJECT))
                .addStatement("return ImmutableMap.<String,Object>builder()$L.build()", sb.toString())
                .build());

        writer.addType(property.build());
    }

    private List<QualifierProcessorExtension> getProcessorExtensions() {
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
            for (; ; ) {
                QualifierProcessorExtension next = null;
                try {
                    if (!iterator.hasNext()) break;
                    next = iterator.next();
                    printMessage("Loaded " + next);
                    builder.add(next);
                } catch (Throwable e) {
                    printError(format("Error loading extension %s, cause: %s\n%s", next,
                            getCausalChain(e).stream().map(Throwable::getLocalizedMessage)
                                    .collect(joining(", caused by ")),
                            getStackTraceAsString(e)));
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

    public Collection<PropertyDescriptor> getPropertyDescriptors(TypeHelper beanHelper) {
        Map<String, MirroringPropertyDescriptor> ps = Maps.newTreeMap();
        final Function<String, MirroringPropertyDescriptor> descriptorFactory = name ->
                new MirroringPropertyDescriptor(beanHelper.getClassRepresenter(), name);
        { // Add self as property
            ps.computeIfAbsent(PropertyDescriptor.SELF, descriptorFactory)
                    .processAnnotationUsingProcessorExtensions(beanHelper.getClassRepresenter());
        }
        for (ExecutableElement method : Iterables.concat(beanHelper.getMethods(), beanHelper.getInnerMethods())) {
            try {
                if (method.getAnnotation(SkipStaticQualifierMetamodelGenerator.class) != null) continue;
                if (method.getEnclosingElement().asType().toString().equals(Object.class.getName())) continue;
                String fullName = method.getSimpleName().toString();
                if (beanHelper.isGetter(method, true)) {
                    String name = toLower(fullName.charAt(0) == 'i' ?
                            fullName.subSequence(2, fullName.length()).toString() :
                            fullName.subSequence(3, fullName.length()).toString());
                    ps.computeIfAbsent(checkValidName(name), descriptorFactory).setGetter(method);
                }
                if (beanHelper.isSetter(method, true)) {
                    String name = toLower(fullName.subSequence(3, fullName.length()).toString());
                    ps.computeIfAbsent(checkValidName(name), descriptorFactory).setSetter(method);
                }
            } catch (Exception e) {
                processingEnv.getMessager().printMessage(ERROR, format("Error during method processing, cause: %s\n%s",
                        getCausalChain(e).stream().map(Throwable::getLocalizedMessage).collect(joining(", caused by ")),
                        getStackTraceAsString(e)), method);
            }
        }

        return Collections.unmodifiableCollection(ps.values());
    }

    public class MirroringPropertyDescriptor implements PropertyDescriptor {
        private final TypeElement beanElement;
        private final DeclaredType beanType;
        private final String name;
        private final QualifierMetadata qualifierMetadata;

        private ExecutableElement getter;
        private ExecutableElement setter;

        public MirroringPropertyDescriptor(TypeElement beanElement, String name) {
            this.beanElement = beanElement;
            this.beanType = MoreTypes.asDeclared(getBeanElement().asType());
            this.name = name;
            this.qualifierMetadata = new MyQualifierMetadata(this);
        }

        @Override public DeclaredType getBeanType() { return beanType; }

        @Override public TypeElement getBeanElement() { return beanElement; }

        /** The property type. Primitive types are returned as boxed. */
        @Override public DeclaredType getPropertyType() {
            if (name.equals(SELF)) return (DeclaredType) getBeanElement().asType();
            // Non SELF properties must have getter or setter, and if both exist the type must match
            assert getter != null || setter != null;
            // TODO next assert fails on static (category) setters
            // assert getter == null || setter == null
            // || types().isSameType(getter.getReturnType(), setter.getParameters().get(0).asType());
            TypeMirror typeMirror = getter != null ? getter.getReturnType() : setter.getParameters().get(0).asType();
            if (typeMirror.getKind().isPrimitive()) {
                typeMirror = types().boxedClass((PrimitiveType) typeMirror).asType();
            }
            return (DeclaredType) typeMirror;
        }

        @Override public @Nullable ExecutableElement getGetterElement() { return getter; }

        public void setGetter(ExecutableElement getter) {
            if (this.getter != null) {
                throw new IllegalStateException("More than one getter for the same property forbidden");
            }
            this.getter = getter;
            processAnnotationUsingProcessorExtensions(getter);
        }

        @Override public @Nullable ExecutableElement getSetterElement() { return setter; }

        public void setSetter(ExecutableElement setter) {
            if (this.setter != null) {
                throw new IllegalStateException("More than one setter for the same property forbidden");
            }
            this.setter = setter;
            processAnnotationUsingProcessorExtensions(setter);
        }

        private void processAnnotationUsingProcessorExtensions(Element method) {
            for (QualifierProcessorExtension extension : getProcessorExtensions()) {
                for (AnnotationAnalyzerEntry<?> e : extension.getSupportedAnnotations()) {
                    processAnnotation(method, e);
                }
            }
        }

        private <A extends Annotation> void processAnnotation(Element annotatedElement, AnnotationAnalyzerEntry<A> e) {
            final Class<A> annotationType = e.annotationType();
            A aClass = annotatedElement.getAnnotation(annotationType);
            AnnotationMirror aMirror = MoreElements.getAnnotationMirror(annotatedElement, e.annotationType()).orNull();
            if (aClass != null && aMirror != null) {
                e.process(qualifierMetadata, annotatedElement, aMirror, aClass);
            }
        }

        @Override public String getName() { return qualifierMetadata.getOrDefault(String.class, CORE_NAME, name); }

        @Override public QualifierMetadata getMetadata() { return qualifierMetadata; }

        @Override public Iterable<QualifierMetadata.Entry> getExtensions() { return getMetadata().getExtensions(); }

        @Override public String toString() {
            return toStringHelper(this).add("name", name).add("getter", getter).add("setter", setter).toString();
        }
    }

    private Types types() { return processingEnv.getTypeUtils();}

    private Elements elements() { return processingEnv.getElementUtils(); }

    private TypeElement typeElementFor(Class<?> clazz) {
        return requireNonNull(elements().getTypeElement(clazz.getCanonicalName()),
                "element for type " + clazz + " not found");
    }

    private TypeName qualifierType(TypeName bean, TypeName property) {
        return ParameterizedTypeName.get(ClassName.get(Qualifier.class), bean, property);
    }

    private TypeName qualifierBase(TypeName bean, TypeName property) {
        return ParameterizedTypeName.get(ClassName.get(BaseQualifier.class), bean, property);
    }

    public void printDigest(String message) {
        if (Boolean.valueOf(processingEnv.getOptions().get("digest"))) {
            processingEnv.getMessager().printMessage(OTHER, message);
        }
    }

    public void printMessage(String msg) { processingEnv.getMessager().printMessage(NOTE, msg); }

    public void printWarning(String msg) { processingEnv.getMessager().printMessage(WARNING, msg, beanElement); }

    public void printError(String msg) { processingEnv.getMessager().printMessage(ERROR, msg, beanElement); }

    public static String toLower(String str) { return CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, str); }

    public static String toUpper(String str) { return CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, str); }

    public static String checkValidName(String word) {
        Preconditions.checkArgument(!RESERVED_JAVA_KEYWORDS.contains(word.toLowerCase()),
                "The property %s contains a reserved java keyword", word);
        return word;
    }

    class MyQualifierMetadata implements QualifierMetadata {
        private final PropertyDescriptor property;
        private final Map<String, Entry> data;

        public MyQualifierMetadata(PropertyDescriptor property) { this.property = property; data = new TreeMap<>(); }

        @Override public <T> T getOrThrow(Class<T> type, String key) { return getOrDefault(type, key, null); }

        @Override public <T> T getOrDefault(Class<T> type, String key, @Nullable T defaultValue) {
            final Entry first = data.get(key);
            return checkNotNull(first != null ? first.getValue(type) : defaultValue, "%s not found", key);
        }

        @Override public <T> void doIfExists(Class<T> type, String key, Consumer<T> apply) {
            final Entry data = this.data.get(key);
            if (data != null) apply.accept(data.getValue(type));
        }

        @Override public void putIfNotNull(String key, @Nullable Object value) {
            Preconditions.checkArgument(!(value instanceof DataExtension), "use specific method instead!");
            if (value != null && !isEmpty(value)) put(key, value);
        }

        @Override public <T> void putIfNotNull(Class<T> type, @Nullable T value) {
            putIfNotNull(type.getName(), value);
        }

        @Override public Entry put(String key, Object value) {
            TypeMirror t = value instanceof TypeMirror ? (TypeMirror) value : typeElementFor(value.getClass()).asType();
            return put(new DataExtension(key, t, value));
        }

        @Override public Entry put(QualifyExtension annotation) {
            return put(new DataExtension(annotation));
        }

        @Override public Entry put(String key, TypeMirror type, String value) {
            return put(new DataExtension(key, type, value));
        }

        @Override public Entry putClass(String key, String className) {
            return put(key, typeElementFor(Class.class).asType(), className);
        }

        @Override public Entry putLiteral(String key, String literalValue) {
            return put(new LiteralExtension(key, literalValue));
        }

        private Entry put(Entry extensionData) {
            data.put(extensionData.getKey(), extensionData);
            return extensionData;
        }

        private boolean isEmpty(Object value) { return value instanceof String && ((String) value).trim().isEmpty(); }

        @Override public boolean contains(String key) { return data.containsKey(key); }

        @Override public Collection<Entry> getExtensions() { return data.values(); }

        public TypeMirror loadType(QualifyExtension annotation) {
            // http://blog.retep.org/2009/02/13/getting-class-values-from-annotations-in-an-annotationprocessor/
            try {
                annotation.type();
                return null; // this must not happens
            } catch (MirroredTypeException exception) {
                return exception.getTypeMirror();
            }
        }

        private class LiteralExtension implements Entry {
            private final String key, literal;

            private LiteralExtension(String key, String literal) { this.key = key; this.literal = literal; }

            @Override public String getKey() { return key; }

            @Override public String toLiteral() { return literal; }

            @Override public TypeMirror getType() { return unsupported("type"); }

            @Override public Object getValue() { return unsupported("value"); }

            @Override public <T> T getValue(Class<T> type) { return unsupported("value"); }

            private <T> T unsupported(String value) {
                throw new UnsupportedOperationException("literal extensions has no processor-time " + value);
            }
        }

        private DataExtension asData(String key, Object val) {
            TypeMirror type = val instanceof TypeMirror ? (TypeMirror) val : typeElementFor(val.getClass()).asType();
            return new DataExtension(key, type, val);
        }

        private class DataExtension implements Entry {
            private final String key;
            private final TypeMirror type;
            private final Object value;
            private final String castValue;

            private DataExtension(QualifyExtension annotation) {
                this(annotation.key(), loadType(annotation), annotation.value());
            }

            private DataExtension(String key, TypeMirror type, Object value) {
                Preconditions.checkArgument(!(value instanceof DataExtension), "please, be careful!");
                this.key = checkNotNull(key, "requires non null keys");
                this.type = type;
                this.value = checkNotNull(value, "requires non null values");

                final String typeString = Splitter.on('<').split(type.toString()).iterator().next();
                switch (typeString) { //@formatter:off
                    case "java.lang.Class": castValue = value + ".class"; break;
                    case "java.lang.String": castValue = "\"" + value + "\""; break;
                    case "java.lang.Integer": castValue = value.toString(); break;
                    default: castValue = typeString + ".valueOf(\"" + value + "\")";
                } //@formatter:on
            }

            @Override public String getKey() { return key; }

            @Override public TypeMirror getType() { return type; }

            @Override public Object getValue() { return value; }

            @Override public <T> T getValue(Class<T> type) {
                Preconditions.checkArgument(types().isAssignable(getType(), typeElementFor(type).asType()),
                        "value type mismatch (%s, key: %s, value: %s):  expected type %s, actual type %s",
                        property, key, getValue(), type.getName(), getType());
                return type.cast(getValue());
            }

            @Override public String toLiteral() { return castValue; }
        }
    }
}
