// Copyright 2013 Intendia, SL.
package com.intendia.qualifier.processor;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.NestingKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

@SuppressWarnings("UnusedDeclaration")
public class TypeHelper {
    private final TypeElement classRepresenter;
    private final ProcessingEnvironment environment;
    private TypeElement[] e;

    public TypeHelper(ProcessingEnvironment environment, TypeElement classRepresenter) {
        this.classRepresenter = classRepresenter;
        this.environment = environment;
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
        return getFlatName(classRepresenter);
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
        return Iterables.filter(getMethods(), TypeHelper.this::isGetter);
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

    private Types types() { return environment.getTypeUtils(); }

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

    public ProcessingEnvironment getProcessingEnvironment() { return environment; }

    public String getSimpleClassName() { return classRepresenter.getSimpleName().toString(); }

    protected Elements getElementUtils() { return environment.getElementUtils(); }

    public TypeMirror getTypeMirror(Class<?> aClass) { return getTypeElement(aClass).asType(); }

    private TypeElement getTypeElement(Class<?> typeClass) {
        return getElementUtils().getTypeElement(typeClass.getName());
    }

    public ExecutableElement getMethod(String name) {
        return getMethods().stream()
                .filter(m -> m.getSimpleName().contentEquals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("method " + name + " not found"));
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
}
