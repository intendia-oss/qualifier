// Copyright 2014 Intendia, SL.
package com.intendia.qualifier.processor;

import static com.intendia.qualifier.Qualifiers.*;

import com.intendia.qualifier.annotation.Representer;

public class RepresenterQualifierProcessor extends AbstractQualifierProcessorExtension
        implements QualifierAnnotationAnalyzer<Representer> {

    public RepresenterQualifierProcessor() {
        addAnnotationAnalyzer(Representer.class, this);
    }

    @Override
    public void processAnnotation(AnnotationContext<Representer> ctx) {
        ctx.getContext().putIfNotNull(REPRESENTER_TEXT_RENDERER, ctx.getAnnotation().textRenderer());
        ctx.getContext().putIfNotNull(REPRESENTER_HTML_RENDERER, ctx.getAnnotation().htmlRenderer());
        ctx.getContext().putIfNotNull(REPRESENTER_CELL, ctx.getAnnotation().cell());
    }
}
