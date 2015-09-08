package com.intendia.qualifier.processor;

import javax.annotation.Nullable;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;

public interface PropertyDescriptor {
    String SELF = "self";

    String getName();

    DeclaredType getBeanType();

    TypeElement getBeanElement();

    DeclaredType getPropertyType();

    @Nullable ExecutableElement getGetterElement();

    @Nullable ExecutableElement getSetterElement();

    QualifierMetadata getMetadata();

    Iterable<QualifierMetadata.Entry> getExtensions();

    default boolean isBean() { return getName().equals(SELF); }
}
