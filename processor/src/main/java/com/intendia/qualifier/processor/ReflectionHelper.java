// Copyright 2013 Intendia, SL.
package com.intendia.qualifier.processor;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.intendia.qualifier.Qualifiers.CORE_NAME;
import static com.intendia.qualifier.processor.AbstractQualifierProcessorExtension.TypedQualifierAnnotationAnalyzerDecorator;
import static java.lang.String.format;

import com.google.common.base.CaseFormat;
import com.google.common.base.Predicate;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.intendia.qualifier.annotation.SkipStaticQualifierMetamodelGenerator;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import javax.annotation.Nullable;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.NestingKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;

@SuppressWarnings("UnusedDeclaration")
public class ReflectionHelper {

    static final String SELF = "self";
    private final TypeElement classRepresenter;
    private final ProcessingEnvironment environment;
    private final List<QualifierProcessorExtension> processorExtensions;
    final DeclaredType classType;
    private TypeElement[] e;

    public ReflectionHelper(ProcessingEnvironment environment, TypeElement classRepresenter,
            List<QualifierProcessorExtension> processorExtensions) {
        this.classRepresenter = classRepresenter;
        this.environment = environment;
        this.processorExtensions = processorExtensions;
        final TypeElement jlcElement = environment.getElementUtils().getTypeElement("java.lang.Class");
        this.classType = environment.getTypeUtils().getDeclaredType(jlcElement);
    }

    public static String toLower(String str) {
        return CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, str);
    }

    public static String toUpper(String str) {
        return CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, str);
    }

    public static String toTitle(String str) {
        if (str.length() == 1) return str.toUpperCase();
        String replace = CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_HYPHEN, str).replace('-', ' ');
        String firstLetter = replace.substring(0, 1).toUpperCase();
        String finalWords = replace.substring(1);
        return firstLetter + finalWords;
    }

    public Collection<VariableElement> filterConstantFields(Collection<VariableElement> fieldElements) {
        return filterFields(fieldElements, Modifier.STATIC, Modifier.FINAL);
    }

    /** Returns only fields which doesn't contain one of the passed modifiers. */
    public Collection<VariableElement> filterFields(Collection<VariableElement> fieldElements, Modifier... modifiers) {
        Collection<VariableElement> filteredFields = new ArrayList<>();
        filteredFields.addAll(fieldElements);
        for (VariableElement fieldElement : fieldElements) {
            for (Modifier modifier : modifiers) {
                if (fieldElement.getModifiers().contains(modifier)) {
                    filteredFields.remove(fieldElement);
                    break;
                }
            }
        }
        return filteredFields;
    }

    /** Returns only fields which doesn't annotated with one of the passed annotation. */
    @SafeVarargs
    public final Collection<VariableElement> filterFields(Collection<VariableElement> fieldElements,
            Class<? extends Annotation>... annotations) {
        Collection<VariableElement> filteredFields = new ArrayList<>();
        filteredFields.addAll(fieldElements);
        for (VariableElement fieldElement : fieldElements) {
            for (Class<? extends Annotation> passedAnnotation : annotations) {
                Annotation fieldAnnotation = fieldElement.getAnnotation(passedAnnotation);
                if (fieldAnnotation != null) {
                    filteredFields.remove(fieldElement);
                    break;
                }
            }
        }
        return filteredFields;
    }

    /** Returns all fields annotated with the passed annotation classes. */
    @SafeVarargs
    public final Collection<VariableElement> getAnnotatedFields(Class<? extends Annotation>... annotations) {
        Collection<VariableElement> fieldsCopy = getFields();
        for (Class<? extends Annotation> annotation : annotations) {
            Collection<VariableElement> nonAnnotatedFields = filterFields(getFields(), annotation);
            fieldsCopy.removeAll(nonAnnotatedFields);
        }
        return fieldsCopy;
    }

    /** Returns the class name. (ex. {@code com.gwtplatform.dispatch.shared.annotation.Foo}) */
    public String getClassName() {
        return classRepresenter.getQualifiedName().toString();
    }

    public String getFlatName() {
        if (classRepresenter.getNestingKind() == NestingKind.MEMBER) {
            return classRepresenter.getEnclosingElement() + "" + getSimpleClassName();
        } else {
            return getClassName();
        }
    }

    public TypeElement getClassRepresenter() {
        return classRepresenter;
    }

    /** Returns all fields ordered that are {@link Modifier#FINAL} or {@link Modifier#STATIC}. */
    public Collection<VariableElement> getConstantFields() {
        return getModifierFields(Modifier.FINAL, Modifier.STATIC);
    }

    /** Returns all fields. */
    public Collection<VariableElement> getFields() {
        List<? extends Element> members = getElementUtils().getAllMembers(classRepresenter);
        return ElementFilter.fieldsIn(members);
    }

    /** Returns all methods. */
    public Collection<ExecutableElement> getMethods() {
        return getExecutableElements(classRepresenter);
    }

    private Collection<ExecutableElement> getExecutableElements(TypeElement classRepresenter) {
        List<? extends Element> members = getElementUtils().getAllMembers(classRepresenter);
        return ElementFilter.methodsIn(members);
    }

    public Collection<TypeElement> getInnerClasses() {
        List<? extends Element> members = getElementUtils().getAllMembers(classRepresenter);
        return ElementFilter.typesIn(members);
    }

    public Collection<ExecutableElement> getInnerMethods() {
        ImmutableList.Builder<ExecutableElement> builder = ImmutableList.builder();
        for (TypeElement typeElement : getInnerClasses()) {
            if (typeElement.getSimpleName().contentEquals("Category")) {
                builder.addAll(getExecutableElements(typeElement));
            }
        }
        return builder.build();
    }

    /** Return all getters methods. */
    public Iterable<ExecutableElement> getGetters() {
        return Iterables.filter(getMethods(), new Predicate<ExecutableElement>() {
            @Override
            public boolean apply(ExecutableElement input) {
                return isGetter(input);
            }
        });
    }

    protected boolean isGetter(ExecutableElement x) {
        return isGetter(x, false);
    }

    protected boolean isGetter(ExecutableElement x, boolean includeStatic) {
        final String name = x.getSimpleName().toString();
        final TypeMirror returnType = x.getReturnType();
        final boolean isStatic = includeStatic && x.getParameters().size() == 1;

        if (x.getParameters().size() != (isStatic ? 1 : 0)) {
            return false;
        }
        if (isStatic && !types().isSameType(x.getParameters().get(0).asType(), getClassRepresenter().asType())) {
            return false;
        }
        if (name.startsWith("get")) {
            return true;
        }
        if (name.startsWith("is") || name.startsWith("has")) {
            if (returnType.getKind().equals(TypeKind.BOOLEAN)) {
                return true;
            }
        }
        if (name.startsWith("is") || name.startsWith("has")) {
            TypeMirror javaLangBoolean = types().boxedClass(types().getPrimitiveType(TypeKind.BOOLEAN)).asType();
            if (returnType.getKind().equals(TypeKind.BOOLEAN) || types().isSameType(returnType, javaLangBoolean)) {
                return true;
            }
        }
        return false;
    }

    private Types types() {
        return environment.getTypeUtils();
    }

    protected boolean isSetter(ExecutableElement x) {
        return isSetter(x, false);
    }

    protected boolean isSetter(ExecutableElement x, boolean includeStatic) {
        final String name = x.getSimpleName().toString();
        final TypeMirror returnType = x.getReturnType();
        final boolean isStatic = includeStatic && x.getParameters().size() == 2;

        if (x.getParameters().size() != (isStatic ? 2 : 1)) {
            return false;
        }
        if (isStatic && !types().isSameType(x.getParameters().get(0).asType(), getClassRepresenter().asType())) {
            return false;
        }
        if (!name.startsWith("set")) {
            return false;
        }
        if (returnType.getKind().equals(TypeKind.VOID)) {
            return true;
        }
        return x.getEnclosingElement() != null && types().isAssignable(x.getEnclosingElement().asType(), returnType);
    }

    /** Returns all fields with the passed modifier. */
    public Collection<VariableElement> getModifierFields(Modifier... modifiers) {
        Collection<VariableElement> modifierFields = new ArrayList<>();
        modifierFields.addAll(getFields());
        for (Modifier modifier : modifiers) {
            Collection<VariableElement> nonModifierFields = filterFields(getFields(), modifier);
            modifierFields.removeAll(nonModifierFields);
        }
        return modifierFields;
    }

    public String getPackageName() {
        return getElementUtils().getPackageOf(classRepresenter).getQualifiedName().toString();
    }

    public ProcessingEnvironment getProcessingEnvironment() {
        return environment;
    }

    public String getSimpleClassName() {
        return classRepresenter.getSimpleName().toString();
    }

    /** Utility method. */
    protected Elements getElementUtils() {
        return environment.getElementUtils();
    }

    public Iterable<? extends QualifierDescriptor> getPropertyDescriptors() {
        SortedMap<String, MirroringQualifierDescriptor> properties = Maps.newTreeMap();
        { // Add self as property
            getOrCreatePropertyDescriptor(properties, SELF)
                    .processAnnotationUsingProcessorExtensions(getClassRepresenter());
        }
        for (ExecutableElement method : Iterables.concat(getMethods(), getInnerMethods())) {
            if (method.getAnnotation(SkipStaticQualifierMetamodelGenerator.class) != null) continue;
            if (method.getEnclosingElement().asType().toString().equals(Object.class.getName())) continue;
            String fullName = method.getSimpleName().toString();
            if (isGetter(method, true)) {
                String name = toLower(fullName.charAt(0) == 'i' ?
                        fullName.subSequence(2, fullName.length()).toString() :
                        fullName.subSequence(3, fullName.length()).toString());
                if (isReservedJavaKeywordsPresent(name)) continue;
                getOrCreatePropertyDescriptor(properties, name).setGetter(method);
            }
            if (isSetter(method, true)) {
                String name = toLower(fullName.subSequence(3, fullName.length()).toString());
                if (isReservedJavaKeywordsPresent(name)) continue;
                getOrCreatePropertyDescriptor(properties, name).setSetter(method);
            }
        }

        return properties.values();
    }

    private MirroringQualifierDescriptor getOrCreatePropertyDescriptor(
            SortedMap<String, MirroringQualifierDescriptor> properties, String name) {
        if (!properties.containsKey(name)) properties.put(name, new MirroringQualifierDescriptor(getClassRepresenter(),
                name));
        return properties.get(name);
    }

    public TypeMirror getTypeMirror(Class<?> aClass) {
        return getTypeElement(aClass).asType();
    }

    public class MirroringQualifierDescriptor implements QualifierDescriptor {
        private final String name;
        private ExecutableElement getter;
        private ExecutableElement setter;
        private QualifierContext qualifierContext;

        public MirroringQualifierDescriptor(TypeElement classRepresenter, String name) {
            this.name = name;
            this.qualifierContext = new QualifierContext(classRepresenter, ReflectionHelper.this);
        }

        public void setGetter(ExecutableElement getter) {
            if (this.getter != null) {
                String msg = "More than one getter for the same property forbidden";
                getProcessingEnvironment().getMessager().printMessage(Diagnostic.Kind.ERROR, msg, getter);
                throw new IllegalStateException(msg);
            }
            this.getter = getter;
            processAnnotationUsingProcessorExtensions(getter);
        }

        @Override @Nullable public ExecutableElement getGetter() { return getter; }

        public void setSetter(ExecutableElement setter) {
            if (this.setter != null) {
                String msg = "More than one setter for the same property forbidden";
                getProcessingEnvironment().getMessager().printMessage(Diagnostic.Kind.ERROR, msg, setter);
                throw new IllegalStateException(msg);
            }
            this.setter = setter;
            processAnnotationUsingProcessorExtensions(setter);
        }

        @Override @Nullable public ExecutableElement getSetter() { return setter; }

        /** The property type. Primitive types are returned as boxed. */
        @Override
        public DeclaredType getType() {
            if (name.equals(SELF)) return (DeclaredType) getClassRepresenter().asType();
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

        private void processAnnotationUsingProcessorExtensions(Element method) {
            for (QualifierProcessorExtension extension : processorExtensions) {
                for (TypedQualifierAnnotationAnalyzerDecorator<?> processor : extension.getSupportedAnnotations()) {
                    processAnnotation(method, processor);
                }
            }
        }

        private <A extends Annotation> void processAnnotation(Element annotatedElement,
                TypedQualifierAnnotationAnalyzerDecorator<A> processor) {
            final Class<A> annotationType = processor.annotationType();
            final A annotation = annotatedElement.getAnnotation(annotationType);
            if (annotation == null) return;

            // Find annotation mirror
            final TypeElement annotationTypeElement = getTypeElement(annotationType);
            for (AnnotationMirror annotationMirror : annotatedElement.getAnnotationMirrors()) {
                if (annotationMirror.getAnnotationType().equals(annotationTypeElement.asType())) {
                    processor.processAnnotation(
                            new AnnotationContext<A>(qualifierContext, annotatedElement, annotationMirror, annotation));
                    return; // annotation mirror found and processed
                }
            }

            getProcessingEnvironment().getMessager().printMessage(Diagnostic.Kind.ERROR,
                    "Annotation mirror not found for annotation " + annotation, annotatedElement);
        }

        @Override public String getName() { return qualifierContext.getOrDefault(String.class, CORE_NAME, name); }

        @Override public QualifierContext getContext() { return qualifierContext; }

        @Override public Iterable<QualifyExtensionData> getExtensions() { return getContext().getExtensions(); }

        @Override public TypeElement getClassRepresenter() { return classRepresenter; }

        @Override public String toString() {
            return toStringHelper(this).add("name", name).add("getter", getter).add("setter", setter).toString();
        }
    }

    private TypeElement getTypeElement(Class<?> typeClass) {
        return getElementUtils().getTypeElement(typeClass.getName());
    }

    private boolean isReservedJavaKeywordsPresent(String word) {
        boolean isPresent = RESERVED_JAVA_KEYWORDS.contains(word.toLowerCase());
        if (isPresent) getProcessingEnvironment().getMessager().printMessage(Diagnostic.Kind.WARNING,
                format("The property %s on resource %s will be ignored because contains a reserved java " +
                        "keyword", word, classRepresenter.getSimpleName()));
        return isPresent;
    }

    /** Represents an unmodifiable set of lowercase reserved words in Java. */
    public static final Set<String> RESERVED_JAVA_KEYWORDS = ImmutableSet.copyOf(Splitter.on(',').split("abstract," +
            "assert,boolean,break,byte,case,catch,char,class,const,continue,default,do,double,else,enum,extends," +
            "false,final,finally,float,for,goto,if,implements,import,instanceof,int,interface,long,native,new,null," +
            "package,private,protected,public,return,short,static,strictfp,super,switch,synchronized,this,throw," +
            "throws,transient,true,try,void,volatile,while"));

}
