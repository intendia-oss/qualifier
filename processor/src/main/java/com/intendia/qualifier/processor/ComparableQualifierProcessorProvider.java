// Copyright 2015 Intendia, SL.
package com.intendia.qualifier.processor;

import com.intendia.qualifier.ComparableQualifier;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import java.util.Comparator;
import javax.lang.model.type.TypeMirror;

public class ComparableQualifierProcessorProvider extends QualifierProcessorServiceProvider {
    @Override public void processProperty(TypeSpec.Builder property, Metamodel ctx) {
        TypeName typeName = TypeName.get(ctx.propertyType());
        property.addSuperinterface(ParameterizedTypeName.get(ClassName.get(ComparableQualifier.class), typeName));

        final TypeMirror comparableType = types().erasure(typeElementFor(Comparable.class).asType());
        final boolean isComparable = types().isAssignable(ctx.propertyType(), comparableType);
        final String comparatorType = isComparable ? "naturalComparator()" : "toStringComparator()";
        property.addMethod(override(ParameterizedTypeName.get(ClassName.get(Comparator.class), typeName),
                "getTypeComparator", "$T.$L", ComparableQualifier.class, comparatorType));
        ctx.metadata().literal(ComparableQualifier.COMPARABLE_COMPARATOR, "getTypeComparator()");
    }
}
