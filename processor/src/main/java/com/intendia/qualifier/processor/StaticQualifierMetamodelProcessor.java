// Copyright 2013 Intendia, SL.
package com.intendia.qualifier.processor;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Throwables.getCausalChain;
import static com.google.common.base.Throwables.getStackTraceAsString;
import static com.google.common.collect.FluentIterable.from;
import static com.intendia.qualifier.processor.Metamodel.SELF;
import static java.lang.String.format;
import static java.util.Collections.unmodifiableCollection;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;
import static javax.lang.model.element.ElementKind.CLASS;
import static javax.lang.model.element.ElementKind.INTERFACE;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;
import static javax.tools.Diagnostic.Kind.ERROR;
import static javax.tools.Diagnostic.Kind.NOTE;
import static javax.tools.Diagnostic.Kind.WARNING;

import com.google.auto.common.MoreElements;
import com.google.auto.common.MoreTypes;
import com.google.common.base.CaseFormat;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.intendia.qualifier.Extension;
import com.intendia.qualifier.PropertyQualifier;
import com.intendia.qualifier.Qualifier;
import com.intendia.qualifier.annotation.Qualify;
import com.intendia.qualifier.annotation.QualifyExtension;
import com.intendia.qualifier.annotation.SkipStaticQualifierMetamodelGenerator;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.WildcardTypeName;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import groovy.lang.GroovyClassLoader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.function.Function;
import javax.annotation.Generated;
import javax.annotation.Nullable;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic.Kind;
import javax.tools.FileObject;
import javax.tools.JavaCompiler;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

/**
 * Static Qualifier Metamodel Processor.
 * <pre>
 *     model: the actual Pojo model, eg. Person
 *     qualify: the annotations over the model, eg. @Unit("years") int getAge()
 *     extension: dynamic @Extension, constant Extension or untyped String, eg. Extension.key("unit")
 *     metadata: collection of extensions, eg. personMetadata.data(UNIT_EXTENSION) == "years"
 *     qualifier: metadata over a type, eg. personMetadata.getType() == Person.class
 *     metamodel: compile time data (model, qualify, extension...)
 * </pre>
 * <b>Qualify</b> is the source of an <b>extension</b> and <b>Qualifier</b> the destination.
 */
public class StaticQualifierMetamodelProcessor extends AbstractProcessor implements Processor {

    /** Represents an unmodifiable set of lowercase reserved words in Java. */
    public static final Set<String> RESERVED_JAVA_KEYWORDS = ImmutableSet.copyOf(Splitter.on(',').split("abstract," +
            "assert,boolean,break,byte,case,catch,char,class,const,continue,default,do,double,else,enum,extends," +
            "false,final,finally,float,for,goto,if,implements,import,instanceof,int,interface,long,native,new,null," +
            "package,private,protected,public,return,short,static,strictfp,super,switch,synchronized,this,throw," +
            "throws,transient,true,try,void,volatile,while"));
    private static final Collection<ElementKind> CLASS_OR_INTERFACE = EnumSet.of(CLASS, INTERFACE);
    public static final WildcardTypeName WILDCARD = WildcardTypeName.subtypeOf(TypeName.OBJECT);
    public static final ClassName LANG_STRING = ClassName.get(String.class);
    private static Set<Element> processed = new HashSet<>();

    private @Nullable List<QualifierProcessorServiceProvider> providers;
    private @Nullable TypeElement beanElement;

    @Override public SourceVersion getSupportedSourceVersion() { return SourceVersion.latestSupported(); }

    @Override public Set<String> getSupportedAnnotationTypes() { return ImmutableSet.of(Qualify.class.getName()); }

    @Override public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment round) {
        print(NOTE, "Processing " + Qualify.class + " " + toStringHelper(round)
                .add("errorRaised", round.errorRaised()).add("processingOver", round.processingOver()));
        for (Element element : round.getElementsAnnotatedWith(Qualify.class)) {
            // Skip conditions
            if (!CLASS_OR_INTERFACE.contains(element.getKind())) {
                print(NOTE, "ignored " + element + ", cause: is not class or interface");
                continue;
            }
            if (processed.contains(element)) {
                print(NOTE, "ignored " + element + ", cause: already processed");
                continue;
            }
            if (element.getAnnotation(SkipStaticQualifierMetamodelGenerator.class) != null) {
                print(NOTE, "ignored " + element + ", cause: marked with @SkipStaticQualifierMetamodelGenerator");
                continue;
            }

            // Prepare processing environment
            this.beanElement = (TypeElement) element;
            try {

                process(); // now!

                processed.add(beanElement);
                print(NOTE, format("Qualifying %s [success]", element));
            } catch (Exception e) {
                final String msg = "Qualifying " + element + " " + " [failure]: " + e + "\n" + getStackTraceAsString(e);
                processingEnv.getMessager().printMessage(ERROR, msg, element);
                print(ERROR, format("Qualifying %s [failure]: %s\n%s", element,
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
        final Collection<Metamodel> qualifiers = getPropertyDescriptors(beanHelper);

        // public abstract class BeanClass extends BaseQualifier<> implements SimpleQualifier<>
        final TypeSpec.Builder container = TypeSpec.classBuilder(metamodelName.simpleName())
                .addOriginatingElement(beanElement)
                .addModifiers(PUBLIC, FINAL)
                .addAnnotation(AnnotationSpec.builder(SuppressWarnings.class)
                        .addMember("value", "$S", "unused")
                        .build())
                .addAnnotation(AnnotationSpec.builder(Generated.class)
                        .addMember("value", "$S", ClassName.get(StaticQualifierMetamodelProcessor.class))
                        .addMember("date", "$S", Instant.now())
                        .addMember("comments", "$S", "Enabled extensions (" + getProviders().size() + "):\n"
                                + getProviders().stream()
                                .map(e -> e.getClass().getSimpleName())
                                .collect(joining("\n")))
                        .build());

        // Private constructor
        container.addMethod(MethodSpec.constructorBuilder().addModifiers(PRIVATE).build());

        // Bean qualifier extensions
        getProviders().stream()
                .filter(QualifierProcessorServiceProvider::processable)
                .forEach(extension -> extension.processBean(container, beanName, qualifiers));

        // Emit qualifiers for each bean property
        for (Metamodel qualifier : qualifiers) {
            emitQualifier(container, qualifier);
        }

        // All qualifiers instance
        {
            final TypeName valueType = qualifierType(beanClassName, WILDCARD);
            final TypeName mapType = ParameterizedTypeName.get(ClassName.get(Collection.class), valueType);
            container.addField(FieldSpec.builder(mapType, "qualifiers", PUBLIC, STATIC, FINAL)
                    .initializer("$[$T.<$T>asList($L)$]", Arrays.class, valueType, qualifiers.stream()
                            .filter(Metamodel::isProperty)
                            .map(p -> format("%1$s", toLower(p.name())))
                            .collect(joining("\n,", "\n", "")))
                    .build());
        }

        JavaFile.builder(beanHelper.getPackageName(), container.build()).build().writeTo(processingEnv.getFiler());
    }

    private void emitQualifier(TypeSpec.Builder writer, Metamodel descriptor) {

        // Bean ex. ref: person, name: self, type: Person
        ClassName beanType = ClassName.get(descriptor.beanElement());

        // Property ex. ref: person.address, name: address, type: Address
        String propertyName = toLower(descriptor.name());
        TypeName propertyType = TypeName.get(descriptor.propertyType());

        // the qualifier representing the property, ex. PersonAddress
        ClassName qualifierType = ClassName.get(beanType.packageName(), beanType.simpleName() + toUpper(propertyName));

        // Property field and class

        final TypeName extendsType = ParameterizedTypeName.get(ClassName.get(Qualifier.class), propertyType);

        // public static final class PersonAddress extends BaseQualifier<Person,Address> {
        final TypeSpec.Builder qualifier = TypeSpec.classBuilder(qualifierType.simpleName())
                .addModifiers(PUBLIC, STATIC, FINAL)
                .addSuperinterface(extendsType);

        getProviders().stream().filter(QualifierProcessorServiceProvider::processable).forEach(extension -> {
            try {
                extension.processProperty(qualifier, descriptor);
            } catch (Throwable e) {
                print(ERROR, format("Processor %s fail processing property %s.%s, cause: %s\n%s",
                        extension.getClass().getSimpleName(), beanType.simpleName(), propertyName,
                        getCausalChain(e).stream()
                                .map(Throwable::getLocalizedMessage)
                                .collect(joining(", caused by ")),
                        getStackTraceAsString(e)));
            }
        });

        // Bean properties
        if (SELF.equals(descriptor.name())) {
            // public Set<Qualifier<? super QualifiedClass, ?>> getPropertyQualifiers() {
            final ParameterizedTypeName propertiesType = ParameterizedTypeName.get(ClassName.get(Collection.class),
                    qualifierType(propertyType, WILDCARD));
            qualifier.addMethod(MethodSpec.methodBuilder("getProperties")
                    .addModifiers(PUBLIC)
                    .returns(propertiesType)
                    .addStatement("return qualifiers")
                    .build());
            descriptor.metadata().literal(Qualifier.CORE_PROPERTIES, "getProperties()");
        }

        // public static final PersonAddress address = new PersonAddress();
        writer.addField(FieldSpec.builder(qualifierType, propertyName, PUBLIC, STATIC, FINAL)
                .initializer("new $T()", qualifierType)
                .build());

        // public final static PersonSelf PersonMetadata = self;
        if (!descriptor.isProperty()) {
            final String metadata = beanType.simpleName() + "Metadata";
            writer.addField(FieldSpec.builder(qualifierType, metadata, PUBLIC, STATIC, FINAL)
                    .initializer(SELF)
                    .build());
        }

        // public PersonAddress address(){ return self.as(address); }
//        if (!!descriptor.isProperty() && !RESERVED_PROPERTIES.contains(propertyName)) {
//            writer.addMethod(MethodSpec.methodBuilder(propertyName)
//                    .addModifiers(PUBLIC, STATIC)
//                    .returns(qualifierType(beanType, propertyType))
//                    .addStatement("return $N.as($N)", PropertyDescriptor.SELF, propertyName)
//                    .build());
//        }

        // Property context
        CodeBlock.Builder entries = CodeBlock.builder();
        final ImmutableList<Metaextension<?>> orderedExtensions = from(descriptor.extensions())
                .filter(p -> !p.extension().isAnonymous() && p.valueBlock().isPresent())
                .toSortedList((e1, e2) -> e1.extension().getKey().compareTo(e2.extension().getKey()));
        for (Metaextension<?> e : orderedExtensions) { // add extensions
            entries.add("case $S: return ", e.extension().getKey());
            entries.add(e.valueBlock().get());
            entries.add(";\n");
        }
        entries.add("default: return null;\n");

        qualifier.addMethod(MethodSpec.methodBuilder("data")
                .addModifiers(PUBLIC)
                .returns(Object.class)
                .addParameter(LANG_STRING, "key")
                .addCode(CodeBlock.builder()
                        .add("switch(key) {$>\n")
                        .add(entries.build())
                        .add("$<}\n").build())
                .build());

        writer.addType(qualifier.build());
    }

    private List<QualifierProcessorServiceProvider> getProviders() {
        if (providers == null) {
            print(NOTE, "Loading qualifier processor providers...");
            final ServiceLoader<QualifierProcessorServiceProvider> loader = ServiceLoader
                    .load(QualifierProcessorServiceProvider.class, getClass().getClassLoader());
            /*
             * This extra-safe loading prevents recursive compilation failures if the processor try to use an not yet
             * compiled extension.
             */
            final ImmutableList.Builder<QualifierProcessorServiceProvider> builder = new ImmutableList.Builder<>();

            // Core extensions (must be executed first)
            builder.add(new QualifyQualifierProcessorProvider());
            builder.add(new PropertyQualifierProcessorProvider());

            final Iterator<QualifierProcessorServiceProvider> iterator = loader.iterator();
            for (; ; ) {
                QualifierProcessorServiceProvider next = null;
                try {
                    if (!iterator.hasNext()) break;
                    next = iterator.next();
                    print(NOTE, "Loaded " + next);
                    builder.add(next);
                } catch (Throwable e) {
                    print(WARNING, format("Error loading qualifier processor service provider %s, cause: %s\n%s", next,
                            getCausalChain(e).stream().map(Throwable::getMessage).collect(joining(", caused by ")),
                            getStackTraceAsString(e)));
                    try {
                        ClassLoader cl = ((JavacProcessingEnvironment) processingEnv).getProcessorClassLoader();
                        GroovyClassLoader groovy = new GroovyClassLoader(cl);
                        String serviceName = e.getMessage().replaceAll("(.*Provider )(.*?)( not found)", "$2");
                        ClassName className = ClassName.bestGuess(serviceName);
                        FileObject resource = processingEnv.getFiler().getResource(StandardLocation.SOURCE_PATH,
                                className.packageName(), className.simpleName().concat(".java"));
                        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
                        // TODO not sure how to configure groovy to use the full class path
                    } catch (Throwable e1) {
                        throw new RuntimeException(e1);
                    }
                }
            }
            final List<QualifierProcessorServiceProvider> extensions = builder.build();
            for (QualifierProcessorServiceProvider extension : extensions) {
                extension.init(processingEnv);
            }
            providers = extensions;
        }
        return this.providers;
    }

    public Collection<Metamodel> getPropertyDescriptors(TypeHelper beanHelper) {
        Map<String, MirroringMetamodel> ps = Maps.newTreeMap();
        final Function<String, MirroringMetamodel> descriptorFactory = name ->
                new MirroringMetamodel(beanHelper.getClassRepresenter(), name);
        { // Add self as property
            ps.computeIfAbsent(SELF, descriptorFactory)
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

        return unmodifiableCollection(ps.values());
    }

    public class MirroringMetamodel implements Metamodel {
        private final TypeElement beanElement;
        private final DeclaredType beanType;
        private final String name;
        private final Metaqualifier metaqualifier;

        private ExecutableElement getter;
        private ExecutableElement setter;

        public MirroringMetamodel(TypeElement beanElement, String name) {
            this.beanElement = beanElement;
            this.beanType = MoreTypes.asDeclared(beanElement().asType());
            this.name = name;
            this.metaqualifier = new LocalMetadata();
        }

        @Override public DeclaredType beanType() { return beanType; }

        @Override public TypeElement beanElement() { return beanElement; }

        /** The property type. Primitive types are returned as boxed. */
        @Override public DeclaredType propertyType() {
            if (name.equals(SELF)) return (DeclaredType) beanElement().asType();
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

        @Override public @Nullable ExecutableElement getterElement() { return getter; }

        public void setGetter(ExecutableElement getter) {
            if (this.getter != null) {
                throw new IllegalStateException("More than one getter for the same property forbidden");
            }
            this.getter = getter;
            processAnnotationUsingProcessorExtensions(getter);
        }

        @Override public @Nullable ExecutableElement setterElement() { return setter; }

        public void setSetter(ExecutableElement setter) {
            if (this.setter != null) {
                throw new IllegalStateException("More than one setter for the same property forbidden");
            }
            this.setter = setter;
            processAnnotationUsingProcessorExtensions(setter);
        }

        private void processAnnotationUsingProcessorExtensions(Element method) {
            getProviders().forEach(extension -> extension.processMethod(method, metadata()));
        }

        @Override public String name() { return name; }

        @Override public Metaqualifier metadata() { return metaqualifier; }

        @Override public String toString() {
            return toStringHelper(this).add("name", name).add("getter", getter).add("setter", setter).toString();
        }
    }

    private Types types() { return processingEnv.getTypeUtils();}

    private Elements elements() { return processingEnv.getElementUtils(); }

    private TypeElement typeElementFor(Class<?> clazz) {
        requireNonNull(clazz.getCanonicalName(), () -> clazz + " has no canonical name (local or anonymous class)");
        return requireNonNull(elements().getTypeElement(clazz.getCanonicalName()),
                "element for type " + clazz + " not found");
    }

    private TypeName qualifierType(TypeName bean, TypeName property) {
        return ParameterizedTypeName.get(ClassName.get(PropertyQualifier.class), bean, property);
    }

    private void print(Kind kind, String msg) { processingEnv.getMessager().printMessage(kind, msg, beanElement); }

    public static String toLower(String str) { return CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, str); }

    public static String toUpper(String str) { return CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, str); }

    public static String checkValidName(String word) {
        Preconditions.checkArgument(!RESERVED_JAVA_KEYWORDS.contains(word.toLowerCase()),
                "The property %s contains a reserved java keyword", word);
        return word;
    }

    class LocalMetadata implements Metaqualifier {
        private final Map<Extension<?>, Metaextension<?>> data = new IdentityHashMap<>();

        @SuppressWarnings("unchecked") public <T> Optional<Metaextension<T>> get(Extension<T> key) {
            return Optional.ofNullable((Metaextension<T>) data.get(key));
        }

        public <T> Metaextension<T> put(Extension<T> key) {
            return get(key).orElseGet(() -> {
                LocalMetaextension<T> e;
                data.put(key, e = new LocalMetaextension<>(key));
                return e;
            });
        }

        public Metaextension<?> use(QualifyExtension annotation) { return extension(annotation); }

        private <T> Metaextension<T> extension(QualifyExtension annotation) {
            final String key = annotation.key();
            final String str = annotation.value();

            TypeMirror typeMirror = extensionType(annotation);
            Class<T> type = extensionClass(typeMirror);

            T value = null;
            if (String.class.equals(type)) {
                //noinspection unchecked
                value = (T) str;
            } else if (type != null) {
                final Method valueOf;
                try {
                    valueOf = type.getMethod("valueOf", String.class);
                    value = type.cast(valueOf.invoke(null, str));
                } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                    print(NOTE, "non processing time extension value (type:" + type + ", value: " + str + "): " + e);
                } catch (Throwable e) {
                    print(NOTE, "error instantiating extension value (type:" + type + ", value: " + str + "): " + e);
                }
            }

            Metaextension<T> metaextension = put(Extension.key(key), typeMirror);
            if (value != null) metaextension.value(value);
            else if (Class.class.equals(type)) metaextension.valueBlock("$T.class", ClassName.bestGuess(str));
            else metaextension.valueBlock("$T.valueOf($S)", typeMirror, str);
            return metaextension;
        }

        private @Nullable <T> Class<T> extensionClass(TypeMirror typeMirror) {
            @Nullable Class<T> type;
            try {
                //noinspection unchecked
                type = (Class<T>) Thread.currentThread().getContextClassLoader().loadClass(typeMirror.toString());
            } catch (Throwable ignore) {
                type = null;
            } return type;
        }

        private TypeMirror extensionType(QualifyExtension annotation) {
            TypeMirror typeMirror;
            try {
                annotation.type(); throw new RuntimeException("unreachable");
            } catch (MirroredTypeException exception) {
                typeMirror = exception.getTypeMirror();
            }
            return typeMirror;
        }

        @Override public Collection<Metaextension<?>> values() { return unmodifiableCollection(data.values()); }

        class LocalMetaextension<T> implements Metaextension<T> {
            private final Extension<T> key;
            private @Nullable TypeMirror type;
            private @Nullable T value;
            private @Nullable CodeBlock block;

            private LocalMetaextension(Extension<T> key) { this.key = key; }

            public Extension<T> extension() { return key; }

            public Optional<TypeMirror> type() { return Optional.ofNullable(type); }

            public LocalMetaextension<T> type(TypeMirror type) {
                Preconditions.checkState(this.type == null, "type already set");
                this.type = type; return this;
            }

            public LocalMetaextension<T> type(Class<T> type) { return type(typeElementFor(type).asType()); }

            public Optional<T> value() { return Optional.ofNullable(value); }

            public LocalMetaextension<T> value(T value) {
                this.value = Preconditions.checkNotNull(value, "value required (extension: %s)", key);
                Class<?> valueClass = value.getClass();
                if (valueClass.getEnclosingClass() != null && valueClass.getEnclosingClass().isEnum()) {
                    valueClass = valueClass.getEnclosingClass();
                }
                TypeMirror valueType = typeElementFor(valueClass).asType();
                if (type == null) type(valueType);
                else Preconditions.checkArgument(types().isSameType(type, valueType),
                        "value type mismatch (extension: %s, expected type: %s, value type: %s)", key, type, valueType);
                return this;
            }

            @Override public Metaextension<T> value(T value, T ignoreIfEqual) {
                if (Objects.equals(value, ignoreIfEqual)) return this; return value(value);
            }

            public Optional<CodeBlock> valueBlock() {
                if (block == null && (type != null && value != null)) {
                    TypeMirror t = type;
                    // try unbox so primitive types are handle by kind
                    try { t = types().unboxedType(type); } catch (Exception ignore) {}
                    switch (t.getKind()) {
                        case BOOLEAN:
                        case BYTE:
                        case SHORT:
                        case INT:
                        case LONG:
                        case FLOAT:
                        case DOUBLE:
                            valueBlock("$L", value);
                            break;
                        case CHAR:
                            valueBlock("'$L'", value);
                            break;
                        case DECLARED:
                            if (types().isSameType(type, typeElementFor(String.class).asType())) {
                                valueBlock("$S", value);
                            } else if (isEnum(type)) {
                                valueBlock("$T.$L", type, value);
                            } else if (types().isSubtype(type, typeElementFor(Number.class).asType())) {
                                valueBlock("$L", value);
                            } else if ("java.lang.Class".equals(types().erasure(type).toString())) {
                                valueBlock("$T.class", value);
                            } else {
                                valueBlock("$T.valueOf($S)", type, value);
                            }
                            break;
                        default:
                            throw new IllegalArgumentException("illegal property type " + type);
                    }
                }

                return Optional.ofNullable(block);
            }

            private boolean isEnum(TypeMirror type) {
                TypeElement element = MoreElements.asType(types().asElement(type));
                TypeMirror superclass = element.getSuperclass();
                return superclass instanceof DeclaredType && "java.lang.Enum"
                        .equals(types().erasure(superclass).toString());
            }

            /** Nullify to not include in static metadata. */
            public LocalMetaextension<T> valueBlock(@Nullable CodeBlock block) {
                this.block = block;
                return this;
            }

            public Metaqualifier done() { return LocalMetadata.this; }
        }
    }
}
