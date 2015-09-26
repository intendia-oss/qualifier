package com.intendia.qualifier.processor;

import javax.annotation.Nullable;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;

public interface Metamodel {
    String SELF = "self";

    String name();

    DeclaredType beanType();

    TypeElement beanElement();

    DeclaredType propertyType();

    @Nullable ExecutableElement getterElement();

    @Nullable ExecutableElement setterElement();

    Metaqualifier metadata();

    default Iterable<Metaextension<?>> extensions() { return metadata().values(); }

    default boolean isProperty() { return !name().equals(SELF); }
}
