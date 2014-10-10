// Copyright 2014 Intendia, SL.
package com.intendia.qualifier.processor;

import static com.intendia.qualifier.extension.RendererExtension.HTML_RENDERER;
import static com.intendia.qualifier.extension.RendererExtension.TEXT_RENDERER;
import static com.intendia.qualifier.processor.ResourceMethod.resourceMethod;

import com.squareup.javawriter.JavaWriter;
import java.io.IOException;

public class GwtRendererQualifierProcessor extends AbstractQualifierProcessorExtension {

    @Override
    public boolean processable() {
        return classExists("com.google.gwt.text.shared.Renderer");
    }

    @Override
    public void processPropertyQualifier(final JavaWriter writer, String beanName, String propertyName,
            final QualifierDescriptor property) throws IOException {
        final String type = writer.compressType(property.getType().toString());
        final QualifierContext context = property.getContext();
        context.doIfExists(String.class, TEXT_RENDERER, resourceMethod(writer, type, "Renderer"));
        context.doIfExists(String.class, HTML_RENDERER, resourceMethod(writer, type, "SafeHtmlRenderer"));
    }

}
