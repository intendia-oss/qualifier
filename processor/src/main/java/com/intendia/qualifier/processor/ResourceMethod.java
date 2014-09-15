// Copyright 2014 Intendia, SL.
package com.intendia.qualifier.processor;

import static java.lang.String.format;
import static java.util.EnumSet.of;
import static javax.lang.model.element.Modifier.PUBLIC;

import com.squareup.javawriter.JavaWriter;
import java.io.IOException;

class ResourceMethod implements QualifierContext.Apply<String> {

    public static ResourceMethod resourceMethod(JavaWriter writer, String propertyType, String resourceType) {
        return new ResourceMethod(writer, propertyType, resourceType);
    }

    private final JavaWriter writer;
    private final String propertyType;

    private final String resourceType;

    private ResourceMethod(JavaWriter writer, String propertyType, String resourceType) {
        this.writer = writer;
        this.propertyType = propertyType;
        this.resourceType = resourceType;
    }

    @Override
    public void apply(String value) throws IOException {
        final String pattern = "return getManager().create%s(this,\"%s\")";
        final String returnType = format("%s<%s>", resourceType, propertyType);
        writer.emitAnnotation(Override.class)
                .beginMethod(returnType, format("get%s", resourceType), of(PUBLIC))
                .emitStatement(pattern, resourceType, value)
                .endMethod().emitEmptyLine();
    }
}
