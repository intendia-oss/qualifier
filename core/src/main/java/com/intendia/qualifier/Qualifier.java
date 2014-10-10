// Copyright 2013 Intendia, SL.
package com.intendia.qualifier;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.intendia.qualifier.Qualifiers.UTIL_COMPARATOR;
import static com.intendia.qualifier.ResourceProvider.ComparatorResourceProvider;

import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import javax.annotation.Nullable;

/** * Based on {@code javax.persistence.metamodel.ManagedType} and {@code java.beans.BeanInfo}. */
public class Qualifier<T> {
    public static <T> Qualifier<T> of (QualifierContext qualifierContext) {
        return new Qualifier<>(qualifierContext);
    }
    private final QualifierContext qualifierContext;

    protected Qualifier(QualifierContext qualifierContext) {
        this.qualifierContext = qualifierContext;
    }

    /** Return the properties context of this qualifier. */
    public QualifierContext getContext() {
        return qualifierContext;
    }

    public static final String CORE_NAME = "core.name";

    public String getName() {
        return getContext().getQualifier(CORE_NAME);
    }

    public static final String CORE_PATH = "core.path";

    public String getPath() {
        return firstNonNull(getContext().<String> getQualifier(CORE_PATH), "");
    }

    public static final String CORE_TYPE = "core.type";

    public Class<T> getType() {
        return getContext().getQualifier(CORE_TYPE);
    }

    public static final String CORE_SUPER = "core.super";

    public Qualifier<? super T> getSuper() {
        return getContext().getQualifier(CORE_SUPER);
    }

    public static final String CORE_PROPERTIES = "core.properties";

    /** Return the property qualifiers of the bean qualifier. */
    public Map<String, PropertyQualifier<T, ?>> getProperties() {
        final Map<String, PropertyQualifier<T, ?>> properties = getContext().getQualifier(CORE_PROPERTIES);
        return firstNonNull(properties, Collections.<String, PropertyQualifier<T, ?>> emptyMap());
    }

    public Comparator<? super T> getComparator() {
        return getContext().getResourceProvider(ComparatorResourceProvider.class, UTIL_COMPARATOR).get(this);
    }

    @Override
    public String toString() {
        return getType() + "Metadata." + getPath();
    }

    public static class StaticQualifierContext implements QualifierContext {
        public static StaticQualifierContext of(QualifierResolver resolver) {
            return new StaticQualifierContext(resolver);
        }

        private final QualifierResolver resolver;

        StaticQualifierContext(QualifierResolver resolver) {
            this.resolver = resolver;
        }

        private QualifierManager getManager() {
            throw new UnsupportedOperationException("QualifierManager is not accessible for static qualifiers");
        }

        protected @Nullable QualifierContext getParent() {
            final Qualifier<?> resolve = (Qualifier<?>) resolver.resolve(CORE_SUPER);
            return resolve == null ? null : resolve.getContext();
        }

        public @Nullable <T> T getQualifier(String extensionKey) {
            Object resolvedValue = resolver.resolve(extensionKey);
            if (resolvedValue != null) {
                // noinspection unchecked
                return (T) resolvedValue;
            }
            return getParent() == null ? null : getParent().<T> getQualifier(extensionKey);
        }

        @Override
        public <T extends ResourceProvider<?>> T getResourceProvider(Class<T> resourceType, String extensionKey) {
            return null; // return getManager().getResourceProvider(resourceType, extensionKey);
        }
    }

    public interface QualifierResolver {
        Object resolve(String extensionKey);
    }
}
