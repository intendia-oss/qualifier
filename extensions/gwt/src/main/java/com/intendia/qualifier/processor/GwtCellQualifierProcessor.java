// Copyright 2014 Intendia, SL.
package com.intendia.qualifier.processor;

import static com.intendia.qualifier.Qualifiers.REPRESENTER_CELL;
import static com.intendia.qualifier.processor.ResourceMethod.resourceMethod;

import com.squareup.javawriter.JavaWriter;
import java.io.IOException;

public class GwtCellQualifierProcessor extends AbstractQualifierProcessorExtension {

    @Override
    public boolean processable() {
        return classExists("com.google.gwt.cell.client.Cell");
    }

    @Override
    public void processPropertyQualifier(JavaWriter writer, String beanName, String propertyName,
            QualifierDescriptor property) throws IOException {
        property.getContext().doIfExists(String.class, REPRESENTER_CELL,
                resourceMethod(writer, writer.compressType(property.getType().toString()), "Cell"));
    }

}
