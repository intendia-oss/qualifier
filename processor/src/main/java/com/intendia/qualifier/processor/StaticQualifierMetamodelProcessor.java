// Copyright 2013 Intendia, SL.
package com.intendia.qualifier.processor;

import static com.google.auto.common.MoreTypes.asDeclared;
import static com.google.auto.common.MoreTypes.asTypeElement;
import static com.google.common.base.CaseFormat.LOWER_CAMEL;
import static com.google.common.base.CaseFormat.UPPER_CAMEL;
import static com.google.common.base.CaseFormat.UPPER_UNDERSCORE;
import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.base.Throwables.getCausalChain;
import static com.google.common.base.Throwables.getStackTraceAsString;
import static com.intendia.qualifier.processor.Metamodel.SELF;
import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableCollection;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;
import static javax.lang.model.element.ElementKind.CLASS;
import static javax.lang.model.element.ElementKind.INTERFACE;
import static javax.lang.model.element.Modifier.DEFAULT;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;
import static javax.tools.Diagnostic.Kind.ERROR;
import static javax.tools.Diagnostic.Kind.NOTE;

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
import com.intendia.qualifier.Metadata;
import com.intendia.qualifier.PropertyQualifier;
import com.intendia.qualifier.Qualifier;
import com.intendia.qualifier.annotation.Qualify;
import com.intendia.qualifier.annotation.Qualify.Link;
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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
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
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.NestingKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.SimpleAnnotationValueVisitor7;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic.Kind;

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
    private static final WildcardTypeName WILDCARD = WildcardTypeName.subtypeOf(TypeName.OBJECT);
    private static final ClassName LANG_STRING = ClassName.get(String.class);
    private static final String PROPERTIES_FIELD = "INSTANCE";
    private static final String PROPERTIES_HOLDER = "PropertiesLazyHolder";
    public static final String PROPERTIES = PROPERTIES_HOLDER + "." + PROPERTIES_FIELD;
    private static Set<Element> processed = new HashSet<>();

    private @Nullable List<QualifierProcessorServiceProvider> providers;

    @Override public SourceVersion getSupportedSourceVersion() { return SourceVersion.latestSupported(); }

    @Override public Set<String> getSupportedAnnotationTypes() {
        return ImmutableSet.of(Qualify.class.getCanonicalName(), Qualify.Auto.class.getCanonicalName());
    }

    @Override public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment round) {
        print(NOTE, "Processing " + Qualify.class + " " + toStringHelper(round)
                .add("errorRaised", round.errorRaised()).add("processingOver", round.processingOver()));

        if (round.processingOver()) return false; // do nothing on processing over

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
            if (element.getAnnotation(Qualify.Skip.class) != null) {
                print(NOTE, "ignored " + element + ", cause: marked with @Qualify.Skip");
                continue;
            }

            // Prepare processing environment
            try {

                process((TypeElement) element); // now!

                processed.add(element);
                print(NOTE, format("Qualifying %s [success]", element), element);
            } catch (Exception e) {
                final String msg = "Qualifying " + element + " " + " [failure]: " + e + "\n" + getStackTraceAsString(e);
                processingEnv.getMessager().printMessage(ERROR, msg, element);
                print(ERROR, format("Qualifying %s [failure]: %s\n%s", element,
                        getCausalChain(e).stream().map(Throwable::getLocalizedMessage).collect(joining(", caused by ")),
                        getStackTraceAsString(e)));
            }
        }

        for (Element element : round.getElementsAnnotatedWith(Qualify.Auto.class)) {
            try {
                String qUCName = element.getSimpleName().toString();
                String qLCName = UPPER_CAMEL.converterTo(LOWER_CAMEL).convert(qUCName);
                String qUUName = UPPER_CAMEL.converterTo(UPPER_UNDERSCORE).convert(qUCName);
                String qName = qUCName + "Qualifier";
                TypeSpec.Builder qualifier = TypeSpec.interfaceBuilder(qName)
                        .addOriginatingElement(element)
                        .addModifiers(PUBLIC)
                        .addAnnotation(FunctionalInterface.class)
                        .addSuperinterface(Metadata.class);
                for (Element e : element.getEnclosedElements()) {
                    if (!(e instanceof ExecutableElement)) continue;
                    ExecutableElement aElement = (ExecutableElement) e;
                    String pLCName = aElement.getSimpleName().toString();
                    String pUUName = LOWER_CAMEL.converterTo(UPPER_UNDERSCORE).convert(pLCName);
                    String pUCName = LOWER_CAMEL.converterTo(UPPER_CAMEL).convert(pLCName);
                    TypeMirror pRetType = aElement.getReturnType();
                    if (pRetType.getKind().isPrimitive()) {
                        pRetType = types().boxedClass((PrimitiveType) pRetType).asType();
                    }
                    boolean isLink = e.getAnnotation(Link.class) != null && MoreTypes.isTypeOf(Class.class, pRetType);

                    String eName = qUUName + "_" + pUUName;
                    String kName = eName + "_KEY";

                    TypeName vTypeName = !isLink ? TypeName.get(pRetType)
                            : ParameterizedTypeName.get(ClassName.get(Qualifier.class),
                            TypeName.get(asDeclared(pRetType).getTypeArguments().get(0)));
                    TypeName eTypeName = ParameterizedTypeName.get(ClassName.get(Extension.class), vTypeName);

                    //String MEASURE_UNIT_OF_MEASURE_KEY = "measure.unit";
                    qualifier.addField(FieldSpec.builder(LANG_STRING, kName, PUBLIC, STATIC, FINAL)
                            .initializer("$S", qLCName + "." + pLCName)
                            .build());
                    //Extension<Unit<?>> MEASURE_UNIT_OF_MEASURE = Extension.key(MEASURE_UNIT_OF_MEASURE_KEY);
                    qualifier.addField(FieldSpec.builder(eTypeName, eName, PUBLIC, STATIC, FINAL)
                            .initializer("$T.key($L)", Extension.class, kName)
                            .build());
                    //default Unit<?> unit() { return data(MEASURE_UNIT_OF_MEASURE, Unit.ONE); }
                    Optional<CodeBlock> defaultLiteral = Optional
                            .ofNullable(aElement.getDefaultValue())
                            .filter(v -> !isLink /*link not supported*/)
                            .map(a -> annotationFieldAsCodeBlock(processingEnv, aElement, aElement.getDefaultValue()));
                    qualifier.addMethod(methodBuilder("get" + qUCName + pUCName)
                            .addModifiers(DEFAULT, PUBLIC)
                            .returns(vTypeName)
                            .addCode("return data($N" + (isLink ? ".as()" : ""), eName)
                            .addCode(!defaultLiteral.isPresent()
                                    ? CodeBlock.builder().add(");\n", eName).build()
                                    : CodeBlock.builder().add(", ").add(defaultLiteral.get()).add(");\n").build())
                            .build());
                }

                // static XxQualifier of(Metadata q) { return q instanceof XxQualifier ? (XxQualifier) q : q::data; }
                String packageName = elements().getPackageOf(element).toString();
                ClassName qType = ClassName.bestGuess(qName);
                qualifier.addMethod(methodBuilder("of")
                        .addModifiers(STATIC, PUBLIC)
                        .returns(qType)
                        .addParameter(Metadata.class, "q")
                        .addStatement("return q instanceof $1T ? ($1T) q : q::data", qType)
                        .build());

                JavaFile.builder(packageName, qualifier.build()).build()
                        .writeTo(processingEnv.getFiler());
            } catch (Exception e) {
                final String msg =
                        "AutoExtension " + element + " " + " [failure]: " + e + "\n" + getStackTraceAsString(e);
                processingEnv.getMessager().printMessage(ERROR, msg, element);
                print(ERROR, format("AutoExtension %s [failure]: %s\n%s", element,
                        getCausalChain(e).stream().map(Throwable::getLocalizedMessage).collect(joining(", caused by ")),
                        getStackTraceAsString(e)));
            }
        }

        return true;
    }

    static CodeBlock annotationFieldAsCodeBlock(ProcessingEnvironment env,
            ExecutableElement annotationMethod, AnnotationValue annotationValue) {
        CodeBlock.Builder builder = CodeBlock.builder();
        annotationValue.accept(new SimpleAnnotationValueVisitor7<Void, Void>() {
            @Override protected Void defaultAction(Object o, Void ignore) {
                builder.add(valueCodeBlock(env, valueType(env, o), o)); return null;
            }

            @Override public Void visitEnumConstant(VariableElement c, Void ignore) {
                builder.add("$T.$L", c.asType(), c.getSimpleName()); return null;
            }

            @Override public Void visitType(TypeMirror t, Void ignore) {
                builder.add("$T.class", t); return null;
            }

            @Override public Void visitArray(List<? extends AnnotationValue> vs, Void ignore) {
                int cnt = vs.size();
                builder.add("new $T{", annotationMethod.getReturnType());
                for (AnnotationValue value : vs) {
                    value.accept(this, null);
                    if (--cnt > 0) builder.add(", ");
                }
                builder.add("}").build();
                return null;
            }
        }, null);
        return builder.build();
    }

    public void process(TypeElement beanElement) throws Exception {
        final TypeHelper beanHelper = new TypeHelper(processingEnv);

        // 'Class' refer full qualified name, 'Name' refer to simple class name
        final String beanName = beanElement.getSimpleName().toString();
        final ClassName beanClassName = ClassName.get(beanElement);
        final ClassName metamodelName = ClassName.bestGuess(getQualifierName(getFlatName(beanElement)));
        final Collection<Metamodel> qualifiers = getPropertyDescriptors(beanElement, beanHelper);

        // Qualifier Metamodel container (non instantiable)
        final TypeSpec.Builder container = TypeSpec.classBuilder(metamodelName.simpleName())
                .addOriginatingElement(beanElement)
                .addModifiers(PUBLIC, FINAL)
                .addAnnotation(AnnotationSpec.builder(SuppressWarnings.class)
                        .addMember("value", "$S", "unused")
                        .build())
                .addAnnotation(AnnotationSpec.builder(Generated.class)
                        .addMember("value", "$S", ClassName.get(StaticQualifierMetamodelProcessor.class))
                        .addMember("date", "$S", Instant.now())
                        .addMember("comments", "$S", "Enabled processor providers (" + getProviders().size() + "):\n"
                                + getProviders().stream()
                                .map(e -> e.getClass().getSimpleName())
                                .collect(joining("\n")))
                        .build())
                .addMethod(MethodSpec.constructorBuilder().addModifiers(PRIVATE).build());

        // Bean qualifier extensions
        getProviders().stream()
                .filter(QualifierProcessorServiceProvider::processable)
                .forEach(extension -> extension.processBean(container, beanName, qualifiers));

        // Emit qualifiers for each bean property
        for (Metamodel qualifier : qualifiers) {
            emitQualifier(beanElement, container, qualifier, metamodelName);
        }

        // All qualifiers instance
        {
            final TypeName valueType = qualifierType(beanClassName, WILDCARD);
            final TypeName mapType = ParameterizedTypeName.get(ClassName.get(Collection.class), valueType);
            container.addType(TypeSpec.classBuilder(PROPERTIES_HOLDER).addModifiers(PRIVATE, STATIC, FINAL)
                    .addField(FieldSpec.builder(mapType, PROPERTIES_FIELD, PRIVATE, STATIC, FINAL)
                            .initializer("$[$T.<$T>asList($L)$]", Arrays.class, valueType, qualifiers.stream()
                                    .filter(Metamodel::isProperty)
                                    .map(p -> format("%1$s", toLower(p.name())))
                                    .collect(joining(",\n", "\n", "")))
                            .build())
                    .build());
        }

        JavaFile.builder(beanHelper.getPackageName(beanElement), container.build()).build()
                .writeTo(processingEnv.getFiler());
    }

    private void emitQualifier(TypeElement beanElement, TypeSpec.Builder writer, Metamodel descriptor,
            ClassName metamodelName) {

        // Bean ex. ref: person, name: self, type: Person
        ClassName beanType = ClassName.get(descriptor.beanElement());

        // Property ex. ref: person.address, name: address, type: Address
        String propertyName = toLower(descriptor.name());
        TypeName propertyType = TypeName.get(descriptor.propertyType());

        // the qualifier representing the property, ex. PersonAddress
        ClassName qualifierType = metamodelName.nestedClass(beanType.simpleName() + toUpper(propertyName));

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
                        getStackTraceAsString(e)), beanElement);
            }
        });

        // Bean properties
        if (SELF.equals(descriptor.name())) {
            descriptor.metadata().literal(Qualifier.CORE_PROPERTIES, "$L", PROPERTIES);
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

        // Property context
        CodeBlock.Builder entries = CodeBlock.builder();

        // Add extensions
        descriptor.extensions().stream()
                .filter(p -> !p.extension().isAnonymous())
                .sorted(Comparator.comparing(o -> o.extension().getKey()))
                .forEachOrdered(e -> {
                    Optional<CodeBlock> codeBlock = e.valueBlock();
                    if (codeBlock.isPresent()) {
                        entries.add("case $S: return ", e.extension().getKey());
                        entries.add(codeBlock.get());
                        entries.add(";\n");
                    }
                });

        List<CodeBlock> mixins = descriptor.mixins();
        if (mixins.size() == 0) entries.add("default: return null;\n");
        else if (mixins.size() == 1) entries.add("default: return ").add(mixins.get(0)).add(".data(key);\n");
        else {
            entries.add("default: Object r = null;$>\n");
            mixins.forEach(m -> entries.add("if (r == null) r = ").add(m).add(".data(key);\n"));
            entries.add("return r;$<\n");
        }

        qualifier.addMethod(methodBuilder("data")
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
            builder.add(new AutoQualifierProcessorProvider());

            final Iterator<QualifierProcessorServiceProvider> iterator = loader.iterator();
            for (; ; ) {
                QualifierProcessorServiceProvider next = null;
                try {
                    if (!iterator.hasNext()) break;
                    next = iterator.next();
                    print(NOTE, "Loaded " + next);
                    builder.add(next);
                } catch (Throwable e) {
                    print(ERROR, format("Error loading QPP %s, cause: %s\n%s", next,
                            getCausalChain(e).stream().map(Throwable::getLocalizedMessage)
                                    .collect(joining(", caused by ")),
                            getStackTraceAsString(e)));
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

    public Collection<Metamodel> getPropertyDescriptors(TypeElement beanElement, TypeHelper beanHelper) {
        Map<String, MirroringMetamodel> ps = Maps.newTreeMap();
        Function<String, MirroringMetamodel> descriptorFactory = name -> new MirroringMetamodel(beanElement, name);

        { // Self as property
            ps.computeIfAbsent(SELF, descriptorFactory).processAnnotated(beanElement);
        }

        // Methods and Categories
        for (ExecutableElement method : Iterables
                .concat(beanHelper.getMethods(beanElement), beanHelper.getInnerMethods(beanElement))) {
            try {
                if (method.getAnnotation(Qualify.Skip.class) != null) continue;
                if (method.getEnclosingElement().asType().toString().equals(Object.class.getName())) continue;
                String fullName = method.getSimpleName().toString();
                if (beanHelper.isGetter(beanElement, method, true)) {
                    String name = toLower(fullName.charAt(0) == 'i'
                            ? /* is */fullName.subSequence(2, fullName.length()).toString()
                            : /* get */fullName.subSequence(3, fullName.length()).toString());
                    ps.computeIfAbsent(checkValidName(name), descriptorFactory).getter(method);
                }
                if (beanHelper.isSetter(beanElement, method, true)) {
                    String name = toLower(/* set */fullName.subSequence(3, fullName.length()).toString());
                    ps.computeIfAbsent(checkValidName(name), descriptorFactory).setter(method);
                }
            } catch (Exception e) {
                processDescriptorError(method, e, "method");
            }
        }

        // Fields
        Qualify beanQualify = beanElement.getAnnotation(Qualify.class);
        Collection<VariableElement> fields = beanQualify != null && beanQualify.fields()
                ? beanHelper.getFields(beanElement) : emptyList();
        for (VariableElement field : fields) {
            try {
                if (field.getAnnotation(Qualify.Skip.class) != null) continue;
                if (field.getModifiers().contains(STATIC) || !field.getModifiers().contains(PUBLIC)) continue;
                String name = field.getSimpleName().toString();
                ps.computeIfAbsent(checkValidName(name), descriptorFactory).field(field);
            } catch (Exception e) {
                processDescriptorError(field, e, "field");
            }
        }

        return unmodifiableCollection(ps.values());
    }
    private void processDescriptorError(Element method, Exception e, String str) {
        print(ERROR, format("Error during " + str + " processing, "
                + "cause: %s\n%s", getCausalChain(e).stream().map(Throwable::getLocalizedMessage)
                .collect(joining(", caused by ")), getStackTraceAsString(e)), method);
    }

    public class MirroringMetamodel implements Metamodel {
        private final TypeElement beanElement;
        private final DeclaredType beanType;
        private final String name;
        private final Metaqualifier metaqualifier;

        private ExecutableElement getter;
        private ExecutableElement setter;
        private VariableElement field;
        private TypeMirror type;
        private @Nullable ClassName mixinMetamodel;
        private @Nullable String mixinProperty;

        public MirroringMetamodel(TypeElement beanElement, String name) {
            this.beanElement = beanElement;
            this.beanType = asDeclared(beanElement().asType());
            this.name = name;
            this.metaqualifier = new LocalMetadata();
        }

        public void getter(ExecutableElement getter) {
            assert this.getter == null : "getter override forbidden";
            this.getter = getter;
            this.type = getter.getReturnType();
            processAnnotated(getter);
        }

        public void setter(ExecutableElement setter) {
            assert this.field == null : "setter override forbidden";
            boolean isStatic = setter.getParameters().size() == 2;
            this.setter = setter;
            this.type = setter.getParameters().get(isStatic ? 1 : 0).asType();
            processAnnotated(setter);
        }

        public void field(VariableElement variable) {
            assert this.field == null : "field override forbidden";
            this.field = variable;
            this.type = variable.asType();
            processAnnotated(field);
        }

        @Override public DeclaredType beanType() { return beanType; }

        @Override public TypeElement beanElement() { return beanElement; }

        /** The property type. Primitive types are returned as boxed. */
        @Override public TypeMirror propertyType() {
            if (name.equals(SELF)) return beanElement().asType();
            TypeMirror typeMirror = type;
            if (typeMirror.getKind().isPrimitive()) {
                typeMirror = types().boxedClass((PrimitiveType) typeMirror).asType();
            }
            return typeMirror;
        }

        @Override public TypeElement propertyElement() {
            TypeMirror typeMirror = propertyType();
            return typeMirror instanceof DeclaredType ? asTypeElement(typeMirror) : null;
        }

        private void processAnnotated(Element element) {
            Qualify.Extend extend = element.getAnnotation(Qualify.Extend.class);
            if (extend != null) {
                if (mixinMetamodel != null) print(Kind.ERROR, "multiple mixin definitions", element);
                TypeElement mixin = qualifyExtendValue(extend);
                if (!mixin.equals(typeElementFor(Object.class))) {
                    mixinMetamodel = ClassName.bestGuess(getQualifierName(getFlatName(mixin)));
                }
                mixinProperty = extend.name().isEmpty() ? name : extend.name();
            }
            getProviders().forEach(extension -> extension.processAnnotated(element, metadata()));
        }

        private TypeElement qualifyExtendValue(Qualify.Extend annotation) {
            TypeElement typeMirror;
            try {
                annotation.value(); throw new RuntimeException("unreachable");
            } catch (MirroredTypeException exception) {
                typeMirror = asTypeElement(exception.getTypeMirror());
            }
            return typeMirror;
        }

        private TypeElement qualifyExtendValue(Qualify annotation) {
            TypeElement typeMirror;
            try {
                annotation.mixin(); throw new RuntimeException("unreachable");
            } catch (MirroredTypeException exception) {
                typeMirror = asTypeElement(exception.getTypeMirror());
            }
            return typeMirror;
        }

        @Override public @Nullable ExecutableElement getterElement() { return getter; }

        @Override public @Nullable ExecutableElement setterElement() { return setter; }

        @Override public @Nullable VariableElement fieldElement() { return field; }

        @Override public String name() { return name; }

        @Override public Metaqualifier metadata() { return metaqualifier; }

        /** The super qualifier user to extend this property metadata. */
        @Override public @Nullable PropertyReference extend() {
            if (mixinMetamodel != null && mixinProperty != null) {
                return new PropertyReference(mixinMetamodel, mixinProperty);
            }

            // apply bean Qualify(mixin) if this property has no explicit @Extend
            if (isProperty()) {
                Qualify beanQualify = beanElement.getAnnotation(Qualify.class);
                TypeElement classRepresenter = qualifyExtendValue(beanQualify);
                if (classRepresenter != null && !classRepresenter.equals(typeElementFor(Object.class))) {
                    return new PropertyReference(
                            ClassName.bestGuess(getQualifierName(getFlatName(classRepresenter))),
                            isNullOrEmpty(mixinProperty) ? name : mixinProperty);
                }
            }

            return null;
        }

        /** List of qualifier that extends this property metadata. */
        @Override public List<CodeBlock> mixins() {
            List<CodeBlock> mixins = new ArrayList<>();

            PropertyReference extend = extend();
            if (extend != null) {
                // this casting ensures that mixin properties matches supper type
                mixins.add(CodeBlock.of("(($T<? super $T>) $T.$L)",
                        Qualifier.class, propertyType(), extend.bean, extend.property));
            }

            // if this property type is qualified, then add it as an mixin
            Optional.ofNullable(propertyElement())
                    .filter(o -> isProperty()) // skip self
                    .filter(o -> o.getAnnotation(Qualify.class) != null)
                    .map(o -> ClassName.bestGuess(getQualifierName(getFlatName(o))))
                    .ifPresent(t -> mixins.add(CodeBlock.of("$T.$L", t, SELF)));

            return mixins;
        }
        @Override public String toString() {
            return toStringHelper(this).add("name", name).add("getter", getter).add("setter", setter).toString();
        }
    }

    private Types types() { return processingEnv.getTypeUtils();}

    private Elements elements() { return processingEnv.getElementUtils(); }

    private TypeElement typeElementFor(Class<?> clazz) {
        return typeElementFor(processingEnv, clazz);
    }

    private TypeName qualifierType(TypeName bean, TypeName property) {
        return ParameterizedTypeName.get(ClassName.get(PropertyQualifier.class), bean, property);
    }

    private void print(Kind kind, String msg) { processingEnv.getMessager().printMessage(kind, msg); }

    private void print(Kind kind, String msg, Element e) { processingEnv.getMessager().printMessage(kind, msg, e); }

    public static String toLower(String str) { return CaseFormat.UPPER_CAMEL.to(LOWER_CAMEL, str); }

    public static String toUpper(String str) { return LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, str); }

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

        public Metaextension<?> use(Qualify.Entry annotation) { return extension(annotation); }

        private <T> Metaextension<T> extension(Qualify.Entry annotation) {
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

        private TypeMirror extensionType(Qualify.Entry annotation) {
            TypeMirror typeMirror;
            try {
                annotation.type(); throw new RuntimeException("unreachable");
            } catch (MirroredTypeException exception) {
                typeMirror = exception.getTypeMirror();
            }
            return typeMirror;
        }

        @Override public Collection<Metaextension<?>> values() { return unmodifiableCollection(data.values()); }

        @Override public void remove(Extension<?> key) { data.remove(key); }

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

                TypeMirror valueType;
                if (value instanceof TypeMirror) {
                    // class in AnnotationMirror.getElementValues
                    valueType = typeElementFor(Class.class).asType();
                } else if (value instanceof VariableElement) {
                    // enums in AnnotationMirror.getElementValues
                    valueType = ((Element) value).asType();
                    valueBlock("$T.$L", valueType, value.toString());
                } else {
                    valueType = valueType(value);
                }

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
                    valueBlock(valueCodeBlock(processingEnv, type, value));
                }

                return Optional.ofNullable(block);
            }

            /** Nullify to not include in static metadata. */
            public LocalMetaextension<T> valueBlock(@Nullable CodeBlock block) {
                this.block = block;
                return this;
            }

            public Metaqualifier done() { return LocalMetadata.this; }

        }

    }

    public TypeMirror valueType(Object value) {
        return valueType(processingEnv, value);
    }

    private static TypeMirror valueType(ProcessingEnvironment env, Object value) {
        TypeMirror valueType;
        Class<?> valueClass = value.getClass();
        if (valueClass.getEnclosingClass() != null && valueClass.getEnclosingClass().isEnum()) {
            valueClass = valueClass.getEnclosingClass();
        }
        valueType = typeElementFor(env, valueClass).asType();
        return valueType;
    }

    public CodeBlock valueCodeBlock(Object value) {
        return valueCodeBlock(this.processingEnv, valueType(value), value);
    }

    public static CodeBlock valueCodeBlock(ProcessingEnvironment env, TypeMirror t, Object value) {
        // try unbox so primitive types are handle by kind
        try { t = env.getTypeUtils().unboxedType(t); } catch (Exception ignore) {}

        CodeBlock.Builder block = CodeBlock.builder();
        switch (t.getKind()) {
            case BOOLEAN:
            case BYTE:
            case SHORT:
            case INT:
            case LONG:
            case FLOAT:
            case DOUBLE:
                block.add("$L", value);
                break;
            case CHAR:
                block.add("'$L'", value);
                break;
            case DECLARED:
                if (MoreTypes.isTypeOf(String.class, t)) {
                    block.add("$S", value);
                } else if (isEnum(t)) {
                    block.add("$T.$L", t, value);
                } else if (env.getTypeUtils().isSubtype(t, typeElementFor(env, Number.class).asType())) {
                    block.add("$L", value);
                } else if (MoreTypes.isTypeOf(Class.class, t)) {
                    block.add("$T.class", value);
                } else {
                    block.add("$T.valueOf($S)", t, value);
                }
                break;
            default:
                throw new IllegalArgumentException("illegal property type " + t);
        }
        return block.build();
    }

    private static boolean isEnum(TypeMirror type) {
        return MoreTypes.isTypeOf(Enum.class, MoreElements.asType(MoreTypes.asElement(type)).getSuperclass());
    }

    public static String getFlatName(TypeElement classRepresenter) {
        if (classRepresenter.getNestingKind() == NestingKind.MEMBER) {
            return classRepresenter.getEnclosingElement() + "" + classRepresenter.getSimpleName().toString();
        } else {
            return classRepresenter.getQualifiedName().toString();
        }
    }

    public static String getQualifierName(String flatName) {
        return flatName + "__";
    }

    static TypeElement typeElementFor(ProcessingEnvironment env, Class<?> clazz) {
        requireNonNull(clazz.getCanonicalName(), () -> clazz + " has no canonical name (local or anonymous class)");
        return requireNonNull(env.getElementUtils().getTypeElement(clazz.getCanonicalName()),
                "element for type " + clazz + " not found");
    }
}
