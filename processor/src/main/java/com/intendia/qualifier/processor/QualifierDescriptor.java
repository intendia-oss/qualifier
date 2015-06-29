package com.intendia.qualifier.processor;

import javax.annotation.Nullable;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;

public interface QualifierDescriptor {
    String getName();

    DeclaredType getType();

    TypeElement getClassRepresenter();

    @Nullable ExecutableElement getGetter();

    @Nullable ExecutableElement getSetter();

    QualifierContext getContext();

    Iterable<QualifyExtensionData> getExtensions();
}
