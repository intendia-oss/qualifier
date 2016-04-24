// Copyright 2013 Intendia, SL.
package com.intendia.qualifier.processor;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

@SuppressWarnings("UnusedDeclaration")
public class TypeHelper {
    private final ProcessingEnvironment environment;
    private TypeElement[] e;

    public TypeHelper(ProcessingEnvironment environment) {
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

    /** Returns all fields. */
    public Collection<VariableElement> getFields(TypeElement element) {
        return ElementFilter.fieldsIn(getElementUtils().getAllMembers(element));
    }

    /** Returns all methods. */
    public Collection<ExecutableElement> getMethods(TypeElement element) {
        return getExecutableElements(element);
    }

    private Collection<ExecutableElement> getExecutableElements(TypeElement classRepresenter) {
        List<? extends Element> members = getElementUtils().getAllMembers(classRepresenter);
        return ElementFilter.methodsIn(members);
    }

    public Collection<TypeElement> getInnerClasses(TypeElement element) {
        return ElementFilter.typesIn(getElementUtils().getAllMembers(element));
    }

    public Collection<ExecutableElement> getInnerMethods(TypeElement element) {
        ImmutableList.Builder<ExecutableElement> builder = ImmutableList.builder();
        for (TypeElement typeElement : getInnerClasses(element)) {
            if (typeElement.getSimpleName().contentEquals("Category")) {
                builder.addAll(getExecutableElements(typeElement));
            }
        }
        return builder.build();
    }

    /** Return all getters methods. */
    public Collection<ExecutableElement> getGetters(TypeElement element) {
        return getMethods(element).stream().filter(m -> isGetter(element, m)).collect(Collectors.toList());
    }

    protected boolean isGetter(TypeElement element, ExecutableElement x) {
        return isGetter(element, x, false);
    }

    protected boolean isGetter(TypeElement element, ExecutableElement x, boolean includeStatic) {
        final String name = x.getSimpleName().toString();
        final TypeMirror returnType = x.getReturnType();
        final boolean isStatic = includeStatic && x.getParameters().size() == 1;

        if (x.getParameters().size() != (isStatic ? 1 : 0)) {
            return false;
        }
        if (isStatic && !types().isSameType(x.getParameters().get(0).asType(), element.asType())) {
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

    protected boolean isSetter(TypeElement element, ExecutableElement x) {
        return isSetter(element, x, false);
    }

    protected boolean isSetter(TypeElement element, ExecutableElement x, boolean includeStatic) {
        final String name = x.getSimpleName().toString();
        final TypeMirror returnType = x.getReturnType();
        final boolean isStatic = includeStatic && x.getParameters().size() == 2;

        if (x.getParameters().size() != (isStatic ? 2 : 1)) {
            return false;
        }
        if (isStatic && !types().isSameType(x.getParameters().get(0).asType(), element.asType())) {
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

    public String getPackageName(TypeElement element) {
        return getElementUtils().getPackageOf(element).getQualifiedName().toString();
    }

    protected Elements getElementUtils() { return environment.getElementUtils(); }
}
