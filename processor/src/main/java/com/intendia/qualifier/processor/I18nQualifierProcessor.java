// Copyright 2014 Intendia, SL.
package com.intendia.qualifier.processor;

import static com.intendia.qualifier.Qualifiers.*;

import com.intendia.qualifier.annotation.I18n;

public class I18nQualifierProcessor extends AbstractQualifierProcessorExtension
        implements QualifierAnnotationAnalyzer<I18n> {

    public I18nQualifierProcessor() {
        addAnnotationAnalyzer(I18n.class, this);
    }

    @Override
    public void processAnnotation(AnnotationContext<I18n> ctx) {
        ctx.getContext().putIfNotNull(I18N_SUMMARY, ctx.getAnnotation().summary());
        ctx.getContext().putIfNotNull(I18N_DESCRIPTION, ctx.getAnnotation().description());
        ctx.getContext().putIfNotNull(I18N_ABBREVIATION, ctx.getAnnotation().abbreviation());
    }

}
