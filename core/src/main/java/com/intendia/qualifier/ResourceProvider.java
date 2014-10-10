// Copyright 2014 Intendia, SL.
package com.intendia.qualifier;

import com.google.gwt.cell.client.Cell;
import com.google.gwt.text.shared.Renderer;
import com.google.gwt.text.shared.SafeHtmlRenderer;
import java.util.Collections;
import java.util.Comparator;
import java.util.Set;
import javax.annotation.Nullable;

public interface ResourceProvider<R> {

    public Class<?> getResourceType();

    /** The qualifiers types ({@link com.intendia.qualifier.Qualifier#getType()}) supported by this provider. */
    public Set<Class<?>> getSupportedTypes();

    public <V> R get(Qualifier<V> qualifier);

    public static abstract class AbstractResourceProvider<R> implements ResourceProvider<R> {
        private final String providerKey;

        protected AbstractResourceProvider(String providerKey) {
            this.providerKey = providerKey;
        }

        public String getProviderKey() {
            return providerKey;
        }

        @Override
        public Set<Class<?>> getSupportedTypes() {
            return Collections.emptySet(); // empty == all types
        }
    }

    public static abstract class StringResourceProvider extends AbstractResourceProvider<String> {
        public StringResourceProvider(String providerKey) {
            super(providerKey);
        }

        public Class<?> getResourceType() {
            return String.class;
        }

        public abstract <V> String get(Qualifier<V> qualifier);
    }

    public static abstract class ComparatorResourceProvider extends AbstractResourceProvider<Comparator<?>> {
        public ComparatorResourceProvider(String providerKey) {
            super(providerKey);
        }

        public Class<?> getResourceType() {
            return Comparator.class;
        }

        public abstract <V> Comparator<V> get(Qualifier<V> qualifier);
    }

    public static abstract class TextRendererResourceProvider extends AbstractResourceProvider<Renderer<?>> {

        public TextRendererResourceProvider(String providerKey) {
            super(providerKey);
        }

        public Class<?> getResourceType() {
            return Renderer.class;
        }

        public abstract <V> Renderer<V> get(Qualifier<V> qualifier);

    }

    public static abstract class HtmlRendererResourceProvider extends AbstractResourceProvider<SafeHtmlRenderer<?>> {

        public HtmlRendererResourceProvider(String providerKey) {
            super(providerKey);
        }

        public Class<?> getResourceType() {
            return SafeHtmlRenderer.class;
        }

        public abstract <V> SafeHtmlRenderer<V> get(Qualifier<V> qualifier);

    }

    public static abstract class CellRendererResourceProvider extends AbstractResourceProvider<Cell<?>> {

        public CellRendererResourceProvider(String providerKey) {
            super(providerKey);
        }

        public Class<?> getResourceType() {
            return Cell.class;
        }

        public abstract <V> Cell<V> get(@Nullable Qualifier<V> qualifier);

    }
}
