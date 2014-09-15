// Copyright 2013 Intendia, SL.
package com.intendia.qualifier;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.isNullOrEmpty;

import com.google.common.annotations.Beta;
import com.google.common.base.Function;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Maps;
import com.google.gwt.cell.client.Cell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.text.shared.Renderer;
import com.google.gwt.text.shared.SafeHtmlRenderer;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.intendia.qualifier.ResourceProvider.CellRendererResourceProvider;
import com.intendia.qualifier.ResourceProvider.HtmlRendererResourceProvider;
import com.intendia.qualifier.ResourceProvider.TextRendererResourceProvider;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import javax.inject.Provider;

/**
 * Maintains a runtime metamodel of beans and properties, a.k.a. qualifiers.
 * <p/>
 * Based on {@code javax.persistence.Persistence}, {@code javax.persistence.EntityManager} and
 * {@code javax.persistence.metamodel.Metamodel}.
 */
@Beta
public interface QualifierManager {

    /**
     * Return the metamodel bean qualifier representing the bean.
     * 
     * @param cls the type of the represented bean or the type of the bean qualifier
     * @throws IllegalArgumentException if not a qualified bean
     */
    public abstract <T> BeanQualifier<T> getBeanQualifier(Class<T> cls);

    /**
     * Return the metamodel bean qualifiers.
     */
    public abstract Set<? extends BeanQualifier<?>> getBeanQualifiers();

    /**
     * Return the renderer associated with the name token. Used to generate plain text representations.
     */
    public abstract <T> Renderer<T> createRenderer(Qualifier<?, T> qualifier, String name);

    /**
     * Return the safe html renderer associated with the name token. Used to generate html representations.
     */
    public abstract <T> SafeHtmlRenderer<T> createSafeHtmlRenderer(Qualifier<?, T> qualifier, String name);

    /**
     * Return the cell associated with the name token. Used to generate cell representations.
     */
    public abstract <T> Cell<T> createCell(Qualifier<?, T> qualifier, String name);

    @Singleton
    static class StandardQualifierManager implements QualifierManager {

        protected static final StaticQualifierLoader STATIC_QUALIFIER_LOADER = GWT.create(StaticQualifierLoader.class);
        // TODO use ClassToInstanceMap
        private final Map<Class<? extends BeanQualifier<?>>, BeanQualifier<?>> qualifierToInstance = Maps.newHashMap();
        private final Map<Class<?>, Class<? extends BeanQualifier<?>>> beanToQualifier = Maps.newHashMap();

        Supplier<Map<String, TextRendererResourceProvider>> textRenderers;
        Supplier<Map<String, HtmlRendererResourceProvider>> htmlRenderers;
        Supplier<Map<String, CellRendererResourceProvider>> cellRenderers;

        StandardQualifierManager() {
            qualifierToInstance.putAll(STATIC_QUALIFIER_LOADER.getBeanQualifiers());
            for (Class<? extends BeanQualifier<?>> qualifierClass : qualifierToInstance.keySet()) {
                beanToQualifier.put(qualifierToInstance.get(qualifierClass).getType(), qualifierClass);
            }
        }

        @Inject
        void inject(
                final Provider<Map<String, TextRendererResourceProvider>> rendererMapProvider,
                final Provider<Map<String, HtmlRendererResourceProvider>> safeHtmlRendererMapProvider,
                final Provider<Map<String, CellRendererResourceProvider>> cellMapProvider) {
            this.textRenderers = Suppliers.memoize(new Supplier<Map<String, TextRendererResourceProvider>>() {
                @Override
                public Map<String, TextRendererResourceProvider> get() {
                    return rendererMapProvider.get();
                }
            });
            this.htmlRenderers = Suppliers
                    .memoize(new Supplier<Map<String, HtmlRendererResourceProvider>>() {
                        @Override
                        public Map<String, HtmlRendererResourceProvider> get() {
                            return safeHtmlRendererMapProvider.get();
                        }
                    });
            this.cellRenderers = Suppliers.memoize(new Supplier<Map<String, CellRendererResourceProvider>>() {
                @Override
                public Map<String, CellRendererResourceProvider> get() {
                    return cellMapProvider.get();
                }
            });
        }

        /**
         * Return the instance metamodel.
         * 
         * @param cls the type of the metamodel
         */
        public <Q extends BeanQualifier<?>> Q materialize(Class<Q> cls) {
            @SuppressWarnings("unchecked") final Q entityType = (Q) qualifierToInstance.get(cls);
            if (entityType == null) {
                throw new IllegalArgumentException("Not a bean qualifier: " + cls);
            }
            return entityType;
        }

        @Override
        public Set<? extends BeanQualifier<?>> getBeanQualifiers() {
            return FluentIterable.from(beanToQualifier.values())
                    .transform(new Function<Class<? extends BeanQualifier<?>>, BeanQualifier<?>>() {
                        @Override
                        public BeanQualifier<?> apply(Class<? extends BeanQualifier<?>> input) {
                            return qualifierToInstance.get(input);
                        }
                    }).toSet();
        }
        
        public <T extends ResourceProvider> T getResourceProvider(Class<T> type, String name) {
            return checkNotNull(doGetResourceProvider(type, name),
                    "Resource provider [type: %s, key: %s] not found", type, name);
        }

        @SuppressWarnings("unchecked")
        public <T extends ResourceProvider> T doGetResourceProvider(Class<T> type, String name) {
            if (TextRendererResourceProvider.class.equals(type)) return (T) textRenderers.get().get(name);
            if (HtmlRendererResourceProvider.class.equals(type)) return (T) htmlRenderers.get().get(name);
            if (CellRendererResourceProvider.class.equals(type)) return (T) cellRenderers.get().get(name);
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
                return getResourceProvider(HtmlRendererResourceProvider.class,"date").get(qualifier);
            }
            return getResourceProvider(HtmlRendererResourceProvider.class, name).get(qualifier);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <V> Cell<V> createCell(Qualifier<?, V> qualifier, String name) {
            return getResourceProvider(CellRendererResourceProvider.class, name).get(qualifier);
        }

        @Override
        public <T> BeanQualifier<T> getBeanQualifier(Class<T> cls) {
            final Class<? extends BeanQualifier<?>> qualifier = beanToQualifier.get(cls);
            checkArgument(qualifier != null, "Not a qualified class: " + cls);
            final BeanQualifier<?> materialize = materialize(qualifier);
            @SuppressWarnings({ "ConstantConditions", "unchecked", "UnnecessaryLocalVariable" })//
            final BeanQualifier<T> beanQualifier = (BeanQualifier<T>) materialize;
            return beanQualifier;
        }
    }

}
