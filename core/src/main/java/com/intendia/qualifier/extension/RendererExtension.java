// Copyright 2014 Intendia, SL.
package com.intendia.qualifier.extension;

import static com.intendia.qualifier.ResourceProvider.HtmlRendererResourceProvider;
import static com.intendia.qualifier.ResourceProvider.TextRendererResourceProvider;

import com.google.gwt.text.shared.Renderer;
import com.google.gwt.text.shared.SafeHtmlRenderer;
import com.intendia.qualifier.Qualifier;
import com.intendia.qualifier.QualifierContext;

public class RendererExtension<T> extends Qualifier<T> {
    public static <T> RendererExtension<T> of(Qualifier<T> qualifier) {
        return new RendererExtension<>(qualifier.getContext());
    }

    protected RendererExtension(QualifierContext qualifierContext) {
        super(qualifierContext);
    }

    String TEXT_RENDERER = "rendererExtension.textRenderer";
    /** Default property renderer. Default implementation return a <code>toString</code> renderer. */
    public Renderer<T> getTextRenderer() {
        // final String key = getContext().getQualifier(TEXT_RENDERER);
        // return getContext().getResourceProvider(TextRendererResourceProvider.class, key).get(this);
        return getContext().getResourceProvider(TextRendererResourceProvider.class, TEXT_RENDERER).get(this);
    }

    String HTML_RENDERER = "rendererExtension.htmlRenderer";
    /** Default property cell. Default implementation return a simple <code>toString</code> cell implementation. */
    public SafeHtmlRenderer<T> getHtmlRenderer() {
        // final String key = getContext().getQualifier(HTML_RENDERER);
        // return getContext().getResourceProvider(SafeHtmlRenderer.class, key).get(this);
        return getContext().getResourceProvider(HtmlRendererResourceProvider.class, HTML_RENDERER).get(this);
    }
}
