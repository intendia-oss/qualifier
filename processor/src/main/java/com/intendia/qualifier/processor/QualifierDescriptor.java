package com.intendia.qualifier.processor;

import static com.intendia.qualifier.processor.ReflectionHelper.QualifyExtensionData;

import javax.annotation.Nullable;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;

public interface QualifierDescriptor {
    String getName();

    TypeMirror getType();

    TypeElement getClassRepresenter();

    @Nullable
    ExecutableElement getGetter();

    @Nullable
    ExecutableElement getSetter();

    QualifierContext getContext();

    Iterable<QualifyExtensionData> getExtensions();
}
