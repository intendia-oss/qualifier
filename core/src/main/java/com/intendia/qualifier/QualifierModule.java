// Copyright 2014 Intendia, SL.
package com.intendia.qualifier;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.collect.Ordering.natural;
import static com.google.common.collect.Ordering.usingToString;
import static com.intendia.qualifier.Qualifiers.DEFAULT_PROVIDER;
import static com.intendia.qualifier.extension.RendererExtension.TEXT_RENDERER;
import static com.intendia.qualifier.Qualifiers.cellWrapper;
import static com.intendia.qualifier.ResourceProvider.ComparatorResourceProvider;
import static com.intendia.qualifier.extension.RendererExtension.DefaultRendererExtension.of;

import com.google.common.collect.Ordering;
import com.google.gwt.cell.client.Cell;
import com.google.gwt.inject.client.AbstractGinModule;
import com.google.gwt.inject.client.binder.GinBinder;
import com.google.gwt.inject.client.multibindings.GinMultibinder;
import com.google.gwt.text.shared.Renderer;
import com.google.gwt.text.shared.SafeHtmlRenderer;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.intendia.qualifier.ResourceProvider.CellRendererResourceProvider;
import com.intendia.qualifier.ResourceProvider.HtmlRendererResourceProvider;
import com.intendia.qualifier.ResourceProvider.TextRendererResourceProvider;
import com.intendia.qualifier.extension.RendererExtension;
import java.util.Comparator;
import java.util.Date;
import javax.annotation.Nullable;

public class QualifierModule extends AbstractGinModule {

    @Override
    protected void configure() {
        bind(QualifierManager.class).to(QualifierManager.StandardQualifierManager.class);
        install(new DefaultResources());
    }

    static class DefaultResources extends AbstractGinModule {

        @Override
        protected void configure() {
            requestStaticInjection(BaseQualifier.class);
            final GinMultibinder<ResourceProvider<?>> providers = getResourceProviderBinder(binder());
            providers.addBinding().to(DefaultTextRendererProvider.class);
            providers.addBinding().to(DefaultHtmlRendererProvider.class);
            providers.addBinding().to(DefaultCellRendererProvider.class);
            providers.addBinding().to(DefaultComparatorProvider.class);
        }

        @Singleton
        static class DefaultTextRendererProvider extends TextRendererResourceProvider {
            DefaultTextRendererProvider() {
                super(DEFAULT_PROVIDER);
            }

            @Override
            public <T> Renderer<T> get(@Nullable Qualifier<T> qualifier) {
                return Qualifiers.rendererWrapper(Qualifiers.<T> createDefaultRepresenter());
            }
        }

        @Singleton
        static class DefaultHtmlRendererProvider extends HtmlRendererResourceProvider {
            @Inject Provider<QualifierManager> qualifierManagerProvider;
            DefaultHtmlRendererProvider() {
                super(DEFAULT_PROVIDER);
            }

            @Override
            public <T> SafeHtmlRenderer<T> get(@Nullable Qualifier<T> qualifier) {
                if (qualifier == null) return Qualifiers.createDefaultSafeHtmlRenderer();
                if (Date.class.equals(qualifier.getType())) return getSafeHtmlRenderer(qualifier);
                final boolean token = isNullOrEmpty(Qualifiers.getString(qualifier, TEXT_RENDERER));
                if (token) return Qualifiers.createDefaultSafeHtmlRenderer();
                else return Qualifiers.safeHtmlRendererWrapper(of(checkNotNull(qualifier)).getTextRenderer());
            }

            @SuppressWarnings("unchecked")
            public <T> SafeHtmlRenderer<T> getSafeHtmlRenderer(Qualifier<T> qualifier) {
                return qualifierManagerProvider.get().getResource(qualifier, SafeHtmlRenderer.class, "date");
            }
        }

        @Singleton
        static class DefaultCellRendererProvider extends CellRendererResourceProvider {
            @Inject Provider<QualifierManager> qualifierManagerProvider;

            DefaultCellRendererProvider() {
                super(DEFAULT_PROVIDER);
            }

            @Override
            public <T> Cell<T> get(@Nullable Qualifier<T> qualifier) {
                if (qualifier == null) return cellWrapper(Qualifiers.<T>createDefaultSafeHtmlRenderer());
                if (Date.class.equals(qualifier.getType())) return cellWrapper(getSafeHtmlRenderer(qualifier));
                else return cellWrapper(of(qualifier).getHtmlRenderer());
            }

            @SuppressWarnings("unchecked")
            public <T> SafeHtmlRenderer<T> getSafeHtmlRenderer(Qualifier<T> qualifier) {
                return qualifierManagerProvider.get().getResource(qualifier, SafeHtmlRenderer.class, "date");
            }
        }

        @Singleton
        static class DefaultComparatorProvider extends ComparatorResourceProvider {
            @Inject
            QualifierManager qualifierManager;

            DefaultComparatorProvider() {
                super(DEFAULT_PROVIDER);
            }

            @Override
            @SuppressWarnings("unchecked")
            public <V> Comparator<V> get(@Nullable Qualifier<V> qualifier) {
                final RendererExtension<V> extension = qualifierManager.asExtension(qualifier, RendererExtension.class);
                return new DefaultComparator<>(extension.getTextRenderer());
            }
        }

    }

    static class DefaultComparator<T> implements Comparator<T> {
        private final static Ordering<Object> objectComparator = usingToString().nullsFirst();
        private final static Ordering<Comparable<?>> comparableComparator = natural().nullsFirst();
        private final static Ordering<String> stringComparator = Ordering.from(String.CASE_INSENSITIVE_ORDER)
                .nullsFirst();
        private final Renderer<T> renderer;

        DefaultComparator(Renderer<T> renderer) {
            this.renderer = renderer;
        }

        @Override
        public int compare(T left, T right) {
            if (left == null || right == null) {
                return objectComparator.compare(left, right);
            } else if (!(left instanceof Comparable<?>)) {
                return stringComparator.compare(renderer.render(left), renderer.render(right));
            } else if (left instanceof String) {
                return stringComparator.compare((String) left, (String) right);
            } else {
                return comparableComparator.compare((Comparable<?>) left, (Comparable<?>) right);
            }
        }
    }

    public static GinMultibinder<ResourceProvider<?>> getResourceProviderBinder(GinBinder binder) {
        return GinMultibinder.newSetBinder(binder, new TypeLiteral<ResourceProvider<?>>() {});
    }

}
