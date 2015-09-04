// Copyright 2013 Intendia, SL.
package com.intendia.qualifier;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.isNullOrEmpty;

import com.google.common.annotations.Beta;
import com.google.gwt.cell.client.Cell;
import com.google.gwt.text.shared.Renderer;
import com.google.gwt.text.shared.SafeHtmlRenderer;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.intendia.qualifier.ResourceProvider.CellRendererResourceProvider;
import com.intendia.qualifier.ResourceProvider.HtmlRendererResourceProvider;
import com.intendia.qualifier.ResourceProvider.TextRendererResourceProvider;
import java.util.Date;
import java.util.Map;

/**
 * Maintains a runtime metamodel of beans and properties, a.k.a. qualifiers.
 * <p>
 * Based on {@code javax.persistence.Persistence}, {@code javax.persistence.EntityManager} and {@code
 * javax.persistence.metamodel.Metamodel}.
 */
@Beta
public interface ResourceManager {

    /** Return the renderer associated with the name token. Used to generate plain text representations. */
    <T> Renderer<T> createRenderer(Qualifier<?, T> qualifier, String name);

    /** Return the safe html renderer associated with the name token. Used to generate html representations. */
    <T> SafeHtmlRenderer<T> createSafeHtmlRenderer(Qualifier<?, T> qualifier, String name);

    /** Return the cell associated with the name token. Used to generate cell representations. */
    <T> Cell<T> createCell(Qualifier<?, T> qualifier, String name);

    @Singleton class StandardResourceManager implements ResourceManager {
        @Inject Map<String, TextRendererResourceProvider> textRenderers;
        @Inject Map<String, HtmlRendererResourceProvider> htmlRenderers;
        @Inject Map<String, CellRendererResourceProvider> cellRenderers;

        public <T extends ResourceProvider> T getResourceProvider(Class<T> type, String name) {
            return checkNotNull(doGetResourceProvider(type, name),
                    "Resource provider [type: %s, key: %s] not found", type, name);
        }

        @SuppressWarnings("unchecked")
        public <T extends ResourceProvider> T doGetResourceProvider(Class<T> type, String name) {
            if (TextRendererResourceProvider.class.equals(type)) return (T) textRenderers.get(name);
            if (HtmlRendererResourceProvider.class.equals(type)) return (T) htmlRenderers.get(name);
            if (CellRendererResourceProvider.class.equals(type)) return (T) cellRenderers.get(name);
            return null;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <V> Renderer<V> createRenderer(Qualifier<?, V> qualifier, String name) {
            return getResourceProvider(TextRendererResourceProvider.class, name).get(qualifier);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <V> SafeHtmlRenderer<V> createSafeHtmlRenderer(Qualifier<?, V> qualifier, String name) {
            if (isNullOrEmpty(name) && Date.class.equals(qualifier.getType())) {
                return getResourceProvider(HtmlRendererResourceProvider.class, "date").get(qualifier);
            }
            return getResourceProvider(HtmlRendererResourceProvider.class, name).get(qualifier);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <V> Cell<V> createCell(Qualifier<?, V> qualifier, String name) {
            return getResourceProvider(CellRendererResourceProvider.class, name).get(qualifier);
        }
    }
}
