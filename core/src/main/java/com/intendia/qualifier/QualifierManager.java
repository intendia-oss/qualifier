// Copyright 2013 Intendia, SL.
package com.intendia.qualifier;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.isNullOrEmpty;
import static com.intendia.qualifier.ResourceProvider.HtmlRendererResourceProvider;

import com.google.common.annotations.Beta;
import com.google.common.base.Function;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Maps;
import com.google.gwt.core.client.GWT;
import com.google.gwt.text.shared.Renderer;
import com.google.gwt.text.shared.SafeHtmlRenderer;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.intendia.qualifier.ResourceProvider.CellRendererResourceProvider;
import com.intendia.qualifier.ResourceProvider.HtmlRendererResourceProvider;
import com.intendia.qualifier.ResourceProvider.TextRendererResourceProvider;
import com.intendia.qualifier.extension.CellExtension;
import com.intendia.qualifier.extension.I18nExtension;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import javax.inject.Provider;

/**
 * Maintains a runtime metamodel of beans and properties, a.k.a. qualifiers.
 * <p/>
 * Based on {@code javax.persistence.Persistence}, {@code javax.persistence.EntityManager} and
 * {@code javax.persistence.metamodel.Metamodel}.
 */
@Beta
public interface QualifierManager {

    public abstract <T> Qualifier<T> getBeanQualifier(Class<T> cls);

    public abstract Set<? extends Qualifier<?>> getBeanQualifiers();

    <T, V> T getResource(Qualifier<V> qualifier, Class<T> type, String key);

    <T> ResourceProvider<T> getResourceProvider(Class<T> type, String key);

    <V, E extends Qualifier<V>> E asExtension(@Nullable Qualifier<V> qualifier, Class<E> extensionType);

    @Singleton
    static class StandardQualifierManager implements QualifierManager {

        protected static final StaticQualifierLoader STATIC_QUALIFIER_LOADER = GWT.create(StaticQualifierLoader.class);
        // TODO use ClassToInstanceMap
        private final Map<Class<? extends Qualifier>, Qualifier<?>> qualifierToInstance = Maps.newHashMap();
        private final Map<Class<?>, Class<? extends Qualifier<?>>> beanToQualifier = Maps.newHashMap();
        private Supplier<Set<ResourceProvider>> resourceProviders;

        StandardQualifierManager() {
            qualifierToInstance.putAll(STATIC_QUALIFIER_LOADER.getBeanQualifiers());
            for (Class<? extends Qualifier<?>> qualifierClass : qualifierToInstance.keySet()) {
                beanToQualifier.put(qualifierToInstance.get(qualifierClass).getType(), qualifierClass);
            }
        }

        @Inject
        void inject(final Provider<Set<ResourceProvider>> resourceProviders) {
            this.resourceProviders = Suppliers.memoize(new Supplier<Set<ResourceProvider>>() {
                @Override
                public Set<ResourceProvider> get() {
                    return resourceProviders.get();
                }
            });
        }

        public <Q extends Qualifier<?>> Q materialize(Class<Q> cls) {
            @SuppressWarnings("unchecked") final Q entityType = (Q) qualifierToInstance.get(cls);
            if (entityType == null) {
                throw new IllegalArgumentException("Not a bean qualifier: " + cls);
            }
            return entityType;
        }

        @Override
        public Set<? extends Qualifier<?>> getBeanQualifiers() {
            return FluentIterable.from(beanToQualifier.values())
                    .transform(new Function<Class<? extends Qualifier<?>>, Qualifier<?>>() {
                        @Override
                        public Qualifier<?> apply(Class<? extends Qualifier<?>> input) {
                            return qualifierToInstance.get(input);
                        }
                    }).toSet();
        }

        @Override
        public <T, V> T getResource(Qualifier<V> qualifier, Class<T> type, String key) {
            return getResourceProvider(type, key).get(qualifier);

        }

        private final Map<Class<?>, ResourceProvider<?>> typeToProviderCache = Maps.newHashMap();

        public <T> ResourceProvider<T> getResourceProvider(Class<T> type, String key) {
            if (!typeToProviderCache.containsKey(type)) {
                for (ResourceProvider<?> resourceProvider : resourceProviders.get()) {
                    if (resourceProvider.getResourceType().equals(type)) {
                        typeToProviderCache.put(type, resourceProvider);
                        break;
                    }
                    typeToProviderCache.put(type, null);
                }
            }

            //noinspection unchecked
            final ResourceProvider<T> reference = (ResourceProvider<T>) typeToProviderCache.get(type);

            return checkNotNull(reference, "Resource provider [type: %s, key: %s] not found", type, key);
        }

//        private final Map<String, Object> resourceProviderCache = Maps.newHashMap();
//
//        @Override
//        @SuppressWarnings("unchecked")
//        public <T extends ResourceProvider> T getResourceProvider(Class<T> type, String key) {
//            final String cacheKey = type + ":" + key;
//            if (!resourceProviderCache.containsKey(cacheKey)) {
//                for (ResourceProvider resourceProvider : resourceProviders.get()) {
//                    if (!resourceProvider.getClass().equals(type)) continue;
//                    if (!resourceProvider.getProviderKey().equals(key)) continue;
//                    resourceProviderCache.put(cacheKey, resourceProvider);
//                    break;
//                }
//                resourceProviderCache.put(cacheKey, null);
//            }
//
//            return (T) checkNotNull(resourceProviderCache.get(cacheKey),
//                    "Resource provider [type: %s, key: %s] not found", type, key);
//        }



        @Override
        public <V, E extends Qualifier<V>> E asExtension(@Nullable Qualifier<V> qualifier, Class<E> extensionType) {
            switch (extensionType.getSimpleName()) {//@formatter:off
                case "CellExtension": return (E) new CellExtension.DefaultCellExtension(qualifier);
                case "CellExtension": return (E) new I18nExtension.De(qualifier);
            }
        }

        @Override
        public <T> Qualifier<T> getBeanQualifier(Class<T> cls) {
            final Class<? extends Qualifier<?>> qualifier = beanToQualifier.get(cls);
            checkArgument(qualifier != null, "Not a qualified class: " + cls);
            final Qualifier<?> materialize = materialize(qualifier);
            @SuppressWarnings({ "ConstantConditions", "unchecked", "UnnecessaryLocalVariable" })//
            final Qualifier<T> beanQualifier = (Qualifier<T>) materialize;
            return beanQualifier;
        }
    }

}
