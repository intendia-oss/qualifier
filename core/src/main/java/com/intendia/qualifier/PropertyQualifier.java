// Copyright 2013 Intendia, SL.
package com.intendia.qualifier;

import java.util.Comparator;
import javax.annotation.Nullable;

/**
 * @param <T> the type of the bean (a.k.a. qualified bean)
 * @param <V> the type of the value (a.k.a qualified property)
 */
@FunctionalInterface
public interface PropertyQualifier<T, V> extends Qualifier<V> {
    Extension<String> PROPERTY_PATH = Extension.key("core.path");

    default String getPath() { return data(PROPERTY_PATH); }

    // TODO add Nullable/Nonnull annotations using processor extensions
    @SuppressWarnings("NullableProblems") default V get(T object) {
        throw new UnsupportedOperationException("Property " + getName() + " is not readable");
    }

    default Boolean isReadable() { return Boolean.FALSE; }

    @SuppressWarnings("NullableProblems") default void set(T object, V value) {
        throw new UnsupportedOperationException("Property " + getName() + " is not settable");
    }

    default Boolean isWritable() { return Boolean.FALSE; }

    // TODO rename as getBeanComparator (or similar, something indicating that this applies to parent)
    default Comparator<? super T> getComparator() {
        return ComparableQualifier.of(this).getOrdering().onResultOf(this::get);
    }

    /** Traverse a qualifier returning a new qualifier which has source type this and value type property. */
    default <U> PropertyQualifier<T, U> as(PropertyQualifier<? super V, U> property) {
        return new PathQualifier<>(this, property);
    }

    static <V> PropertyQualifier<V, V> asProperty(Qualifier<V> q) { return new SelfPropertyQualifier<>(q); }

    class SelfPropertyQualifier<V> implements PropertyQualifier<V, V> {
        private final Qualifier<V> self;

        private SelfPropertyQualifier(Qualifier<V> self) { this.self = self; }

        @Nullable @Override public Object data(String key) { return self.data(key); }

        @Override public @Nullable V get(V object) { return object; }

        @Override public Boolean isReadable() { return true; }

        @Override public Comparator<? super V> getComparator() { return ComparableQualifier.of(self).getOrdering(); }
    }
}
