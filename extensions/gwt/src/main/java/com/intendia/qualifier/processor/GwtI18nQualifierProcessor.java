// Copyright 2014 Intendia, SL.
package com.intendia.qualifier.processor;

import static com.google.gwt.i18n.client.Constants.DefaultStringValue;
import static com.google.gwt.i18n.client.LocalizableResource.Key;
import static com.intendia.qualifier.Qualifiers.*;
import static com.intendia.qualifier.processor.ReflectionHelper.toLower;
import static com.intendia.qualifier.processor.ReflectionHelper.toTitle;
import static java.lang.String.format;
import static java.util.EnumSet.of;
import static javax.lang.model.element.Modifier.*;

import com.google.common.collect.ImmutableMap;
import com.google.gwt.i18n.client.Constants;
import com.google.gwt.i18n.client.LocalizableResource;
import com.squareup.javawriter.JavaWriter;
import java.io.IOException;

public class GwtI18nQualifierProcessor extends AbstractQualifierProcessorExtension {

    private static final String KEY_FORMAT = "\"metamodel.%s.%s\"";
    public static final String CONSTANTS = "constants";

    @Override
    public boolean processable() {
        return classExists("com.google.gwt.i18n.client.LocalizableResource");
    }

    @Override
    public void processBeanQualifier(final JavaWriter writer, final String beanName,
            Iterable<? extends QualifierDescriptor> properties) throws IOException {
        final String constantsClass = writer.compressType(beanName + "Constants");
        writer.emitAnnotation(LocalizableResource.Generate.class.getSimpleName(), ImmutableMap.of("format",
                "\"com.intendia.igestion.server.i18n.ApplicationCatalogFactory\"", "locales", "\"default\""));
        writer.emitAnnotation(LocalizableResource.GenerateKeys.class.getSimpleName(),
                "\"com.intendia.igestion.server.i18n.ApplicationKeyGenerator\"");
        writer.beginType(constantsClass, "interface", of(PUBLIC, STATIC), Constants.class.getName()).emitEmptyLine();

        for (final QualifierDescriptor property : properties) {
            final String name = property.getName();
            final QualifierContext context = property.getContext();

            if (!context.contains(I18N_SUMMARY)) context.put(I18N_SUMMARY, defaultSummary(property));
            context.doIfExists(String.class, I18N_SUMMARY, new QualifierContext.Apply<String>() {
                @Override
                public void apply(String summary) throws IOException {
                    final String key = name + "";
                    writer.emitAnnotation(DefaultStringValue.class.getSimpleName(), format("\"%s\"", summary))
                            .emitAnnotation(Key.class.getSimpleName(), format(KEY_FORMAT, toLower(beanName), key))
                            .beginMethod("String", getConstantPropertyName(beanName, key), of(PUBLIC, ABSTRACT))
                            .endMethod().emitEmptyLine();
                }
            });
            context.doIfExists(String.class, I18N_ABBREVIATION, new QualifierContext.Apply<String>() {
                @Override
                public void apply(String abbr) throws IOException {
                    final String key = name + "Abbreviation";
                    writer.emitAnnotation(DefaultStringValue.class.getSimpleName(), format("\"%s\"", abbr))
                            .emitAnnotation(Key.class.getSimpleName(), format(KEY_FORMAT, toLower(beanName), key))
                            .beginMethod("String", getConstantPropertyName(beanName, key), of(PUBLIC, ABSTRACT))
                            .endMethod().emitEmptyLine();
                }
            });
            context.doIfExists(String.class, I18N_DESCRIPTION, new QualifierContext.Apply<String>() {
                @Override
                public void apply(String desc) throws IOException {
                    final String key = name + "Title";
                    writer.emitAnnotation(DefaultStringValue.class.getSimpleName(), format("\"%s\"", desc))
                            .emitAnnotation(Key.class.getSimpleName(), format(KEY_FORMAT, toLower(beanName), key))
                            .beginMethod("String", getConstantPropertyName(beanName, key), of(PUBLIC, ABSTRACT))
                            .endMethod().emitEmptyLine();
                }
            });
        }
        writer.endType();
        // Constants singleton instance
        String initialValue = String.format("GWT.isClient() ? GWT.<%1$s>create(%1$s.class) : null", constantsClass);
        writer.emitField(constantsClass, CONSTANTS, of(PRIVATE, STATIC, FINAL), initialValue).emitEmptyLine();
    }

    public String defaultSummary(QualifierDescriptor property) {
        return property.getName().equals(ReflectionHelper.SELF)
                ? toTitle(property.getClassRepresenter().getSimpleName().toString())
                : toTitle(property.getName());
    }

    @Override
    public void processPropertyQualifier(JavaWriter writer, String beanName, String propertyName,
            QualifierDescriptor property) throws IOException {
        final String name = getConstantPropertyName(beanName, propertyName);
        final boolean description = property.getContext().contains(I18N_DESCRIPTION);
        final boolean abbreviation = property.getContext().contains(I18N_ABBREVIATION);

        overrideMethod(writer, "String", "summary", "%s.%s()", CONSTANTS, name);
        if (description) overrideMethod(writer, "String", "description", "%s.%sTitle()", CONSTANTS, name);
        if (abbreviation) overrideMethod(writer, "String", "abbreviation", "%s.%sAbbreviation()", CONSTANTS, name);
    }

    private String getConstantPropertyName(String proxySimpleName, String propertyName) {
        return toLower(proxySimpleName) + ReflectionHelper.toUpper(propertyName);
    }

}
